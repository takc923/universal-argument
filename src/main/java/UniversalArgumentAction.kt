import com.intellij.codeInsight.editorActions.PasteHandler
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.*
import com.intellij.openapi.ui.Messages
import com.intellij.util.Producer
import java.awt.datatransfer.Transferable

class UniversalArgumentAction : EditorAction(Handler()) {

    internal class Handler : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
            if (!ourActionsRegistered) {
                registerActions()
                ourActionsRegistered = true
            }

            if (state.isEnabled() && repeatCount > 0) {
                state = State.ENABLED_AFTER_NUM_INPUT
            } else {
                repeatCount = 0
                state = State.ENABLED
            }
        }

        class MyTypedHandler(originalHandler: TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {
            override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
                when {
                    state.isDisabled() -> myOriginalHandler?.execute(editor, charTyped, dataContext)
                    charTyped.isDigit() && !state.isEnabledAfterNumInput() -> {
                        repeatCount = charTyped.toString().toInt() + repeatCount * 10
                        HintManager.getInstance().showInformationHint(editor, "$repeatCount")
                    }
                    else -> repeatAction { myOriginalHandler?.execute(editor, charTyped, dataContext) }
                }
            }

        }

        class MyEditorActionHandler(private val myOriginalHandler: EditorActionHandler) : EditorActionHandler() {
            override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) = myDoExecute(myOriginalHandler, editor, caret, dataContext)
            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean = state.isEnabled() || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }

        class MyEditorWriteActionHandler(private val myOriginalHandler: EditorWriteActionHandler) : EditorWriteActionHandler() {
            override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) = myDoExecute(myOriginalHandler, editor, caret, dataContext)
            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean = state.isEnabled() || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }

        class MyPasteActionHandler(private val myOriginalHandler: PasteHandler) : PasteHandler(myOriginalHandler) {
            override fun execute(editor: Editor?, dataContext: DataContext?, producer: Producer<Transferable>?) = myOriginalHandler.execute(editor, dataContext, producer)
            override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) = myDoExecute(myOriginalHandler, editor, caret, dataContext)
            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean = state.isEnabled() || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }

        class MyEscapeEditorActionHandler(private val myOriginalHandler: EditorActionHandler) : EditorActionHandler() {
            public override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
                if (state.isEnabled()) {
                    state = State.DISABLED
                    repeatCount = 0
                } else {
                    myOriginalHandler.execute(editor, caret, dataContext)
                }
            }

            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean =
                    state.isEnabled() || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }


        companion object {
            private var ourActionsRegistered = false
            private var repeatCount = 0
            private var state = State.DISABLED

            enum class State {
                ENABLED {
                    override fun isEnabled(): Boolean = true
                },
                ENABLED_AFTER_NUM_INPUT {
                    override fun isEnabled(): Boolean = true
                },
                DISABLED {
                    override fun isEnabled(): Boolean = false
                };

                abstract fun isEnabled(): Boolean
                fun isDisabled(): Boolean = !isEnabled()
                fun isEnabledAfterNumInput(): Boolean = this == ENABLED_AFTER_NUM_INPUT
            }

            private fun repeatAction(action: (kotlin.Int) -> kotlin.Unit) {
                state = State.DISABLED
                val targetRepeatCount = when (repeatCount) {
                    0 -> 4
                    else -> repeatCount
                }
                repeatCount = 0

                val answer = when {
                    targetRepeatCount < 1000 -> Messages.OK
                    else -> Messages.showDialog("Are you sure?",
                            "This operation can hang up",
                            arrayOf(Messages.OK_BUTTON, Messages.CANCEL_BUTTON),
                            1,
                            Messages.getWarningIcon(),
                            null)
                }

                if (answer == Messages.OK) repeat(targetRepeatCount) { action.invoke(it) }
            }

            private fun myDoExecute(myOriginalHandler: EditorActionHandler, editor: Editor, caret: Caret?, dataContext: DataContext) =
                    if (state.isEnabled()) repeatAction { myOriginalHandler.execute(editor, caret, dataContext) }
                    else myOriginalHandler.execute(editor, caret, dataContext)

            private fun registerActions() {
                val editorActionManager = EditorActionManager.getInstance()

                val typedAction = editorActionManager.typedAction
                typedAction.setupRawHandler(MyTypedHandler(typedAction.rawHandler))

                val actionManager = ActionManager.getInstance()

                for (actionId in actionManager.getActionIds("")) {
                    val action = actionManager.getAction(actionId) as? EditorAction ?: continue
                    val handler = action.handler
                    val newHandler = when {
                        action is UniversalArgumentAction -> null
                        actionId == IdeActions.ACTION_EDITOR_ESCAPE -> MyEscapeEditorActionHandler(handler)
                        handler is EditorWriteActionHandler -> MyEditorWriteActionHandler(handler)
                        handler is PasteHandler -> MyPasteActionHandler(handler)
                        handler is EditorActionHandler -> MyEditorActionHandler(handler)
                        else -> null
                    }
                    newHandler ?: continue

                    editorActionManager.setActionHandler(actionId, newHandler)
                }
            }
        }
    }
}
