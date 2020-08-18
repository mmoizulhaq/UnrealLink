package com.jetbrains.rider.plugins.unreal

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.find.SearchReplaceComponent
import com.intellij.ide.actions.NextOccurenceToolbarAction
import com.intellij.ide.actions.PreviousOccurenceToolbarAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.plugins.unreal.actions.FilterCheckboxAction
import com.jetbrains.rider.plugins.unreal.actions.FilterComboAction
import com.jetbrains.rider.plugins.unreal.toolWindow.UnrealToolWindowFactory
import com.jetbrains.rider.ui.components.ComponentFactories
import java.awt.BorderLayout
import javax.swing.JPanel

class UnrealPane(val model: Any, lifetime: Lifetime, val project: Project) : SimpleToolWindowPanel(false) {
    private val consoleView: ConsoleViewImpl = ComponentFactories.getConsoleView(project, lifetime)

    companion object {
        lateinit var currentConsoleView : ConsoleViewImpl
        val verbosityFilterActionGroup: FilterComboAction = FilterComboAction("Verbosity")
        val categoryFilterActionGroup: FilterComboAction = FilterComboAction("Categories")
        val timestampCheckBox: JBCheckBox = JBCheckBox("Show timestamps", false)
        lateinit var filter: SearchReplaceComponent
    }

    init {
        currentConsoleView = consoleView
        currentConsoleView.setUpdateFoldingsEnabled(true)

        val actionGroup = DefaultActionGroup().apply {

            addAll(consoleView.createConsoleActions()
                    .filter {
                        !(it is PreviousOccurenceToolbarAction ||
                                it is NextOccurenceToolbarAction/* || it is ConsoleViewImpl.ClearAllAction*/)
                    }.toList())
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("", actionGroup, myVertical).component

        verbosityFilterActionGroup.addItem(FilterCheckboxAction("Errors", true))
        verbosityFilterActionGroup.addItem(FilterCheckboxAction("Warnings", true))
        verbosityFilterActionGroup.addItem(FilterCheckboxAction("Messages", true))
        categoryFilterActionGroup.addItem(FilterCheckboxAction("All", true))
        val topGroup = DefaultActionGroup().apply {
            add(verbosityFilterActionGroup)
            add(categoryFilterActionGroup)
        }
        val topToolbar = ActionManager.getInstance().createActionToolbar("", topGroup, true).component

        val topPanel = JPanel(HorizontalLayout(0))
        topPanel.add(topToolbar)

        filter = SearchReplaceComponent.buildFor(project, consoleView.editor.contentComponent).withDataProvider(consoleView).build()
        for (component in filter.components) {
            if (component is OnePixelSplitter) {
                for (innerComponent in component.components) {
                    if (innerComponent is NonOpaquePanel || innerComponent !is JPanel) {
                        component.remove(innerComponent)
                    }
                }
                break
            }
        }

        topPanel.add(filter)
        topPanel.add(timestampCheckBox)

        consoleView.scrollTo(0)

        consoleView.add(topPanel, BorderLayout.NORTH)
        setContent(consoleView)
        setToolbar(toolbar)

        UnrealToolWindowFactory.getInstance(project).onUnrealPaneCreated()
    }
}