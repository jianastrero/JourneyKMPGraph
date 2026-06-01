package dev.jianastrero.scanner

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import dev.jianastrero.model.ExitEdge
import dev.jianastrero.model.JourneyGraph
import dev.jianastrero.model.PiggybackInfo
import dev.jianastrero.model.StepNode
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

// @Journey has SOURCE retention — not in bytecode — so we scan KtFiles directly via PSI.
class PsiJourneyScanner : JourneyScanner {
    override fun scan(project: Project): List<JourneyGraph> {
        val scope = GlobalSearchScope.projectScope(project)
        val ktFileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")
        val psiManager = PsiManager.getInstance(project)
        return FileTypeIndex.getFiles(ktFileType, scope).flatMap { vFile ->
            try {
                val psiFile = psiManager.findFile(vFile) ?: return@flatMap emptyList()
                PsiTreeUtil.findChildrenOfType(psiFile, KtClass::class.java)
                    .filter { it.annotationEntries.any { a -> a.shortName?.asString() == "Journey" } }
                    .mapNotNull { tryParseJourney(it) }
            } catch (e: ProcessCanceledException) {
                throw e  // must not swallow PCE — it drives IntelliJ's cancellation mechanism
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun tryParseJourney(ktClass: KtClass): JourneyGraph? = try {
        parseJourney(ktClass)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
        null
    }

    private fun parseJourney(ktClass: KtClass): JourneyGraph {
        val steps = ktClass.body?.declarations
            ?.filterIsInstance<KtClassOrObject>()
            ?.filter { it.annotationEntries.any { a -> a.shortName?.asString() == "Step" } }
            ?: emptyList()

        // Parse annotations once per step — reused for both node building and edge extraction
        val exitsByStep      = steps.associateWith { s -> s.annotationEntries.filter { it.shortName?.asString() == "Exit" } }
        val piggybacksByStep = steps.associateWith { s ->
            s.annotationEntries
                .filter { it.shortName?.asString() == "Piggyback" }
                .mapNotNull { pb ->
                    val id = pb.valueArguments.getOrNull(0)?.getArgumentExpression()?.text?.trim('"')
                        ?: return@mapNotNull null
                    val triggerText = pb.valueArguments.getOrNull(1)?.getArgumentExpression()?.text ?: "ON_ENTER"
                    PiggybackInfo(id, if (triggerText.contains("ON_EXIT")) "ON_EXIT" else "ON_ENTER")
                }
        }

        val nodes = steps.mapIndexed { i, s ->
            StepNode(
                name       = s.name ?: "?",
                isInitial  = i == 0,
                isTerminal = exitsByStep[s].isNullOrEmpty(),
                piggybacks = piggybacksByStep[s] ?: emptyList()
            )
        }

        val edges = steps.flatMap { s ->
            exitsByStep[s].orEmpty().mapNotNull { exit ->
                val args   = exit.valueArguments
                val label  = args.getOrNull(0)?.getArgumentExpression()?.text?.trim('"') ?: return@mapNotNull null
                val target = args.getOrNull(1)?.getArgumentExpression()?.text?.removeSuffix("::class") ?: return@mapNotNull null
                ExitEdge(from = s.name ?: "?", to = target, label = label)
            }
        }

        return JourneyGraph(name = ktClass.name ?: "Unknown", steps = nodes, edges = edges)
    }
}
