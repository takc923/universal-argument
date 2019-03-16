import com.intellij.codeInsight.editorActions.PasteHandler
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
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

            if (state.isEnabled() && count > 0) {
                state = State.ENABLED_AFTER_NUM_INPUT
            } else {
                count = 0
                state = State.ENABLED
            }
        }

        class MyTypedHandler(originalHandler: TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {
            override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
                when {
                    state.isDisabled() -> myOriginalHandler?.execute(editor, charTyped, dataContext)
                    charTyped.isDigit() && !state.isEnabledAfterNumInput() -> {
                        count = charTyped.toString().toInt() + count * 10
                        HintManager.getInstance().showInformationHint(editor, "$count")
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
                    count = 0
                } else {
                    myOriginalHandler.execute(editor, caret, dataContext)
                }
            }

            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean =
                    state.isEnabled() || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }


        companion object {
            private var ourActionsRegistered = false
            private var count = 0
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
                val targetCount = when (count) {
                    0 -> 4
                    else -> count
                }
                count = 0

                val answer = when {
                    targetCount < 1000 -> Messages.OK
                    else -> Messages.showDialog("This operation can hang up",
                            "Are you sure?",
                            arrayOf(Messages.OK_BUTTON, Messages.CANCEL_BUTTON),
                            1,
                            Messages.getWarningIcon(),
                            null)
                }

                if (answer == Messages.OK) repeat(targetCount) { action.invoke(it) }
            }

            private fun myDoExecute(myOriginalHandler: EditorActionHandler, editor: Editor, caret: Caret?, dataContext: DataContext) =
                    if (state.isEnabled()) repeatAction { myOriginalHandler.execute(editor, caret, dataContext) }
                    else myOriginalHandler.execute(editor, caret, dataContext)

            private fun registerActions() {
                val actionManager = EditorActionManager.getInstance()

                val typedAction = actionManager.typedAction
                typedAction.setupRawHandler(MyTypedHandler(typedAction.rawHandler))

                for (action in supportedActions) {
                    val handler = actionManager.getActionHandler(action)
                    when (handler) {
                        is EditorWriteActionHandler -> actionManager.setActionHandler(action, MyEditorWriteActionHandler(handler))
                        is PasteHandler -> actionManager.setActionHandler(action, MyPasteActionHandler(handler))
                        else -> actionManager.setActionHandler(action, MyEditorActionHandler(handler))
                    }
                }
                actionManager.setActionHandler(IdeActions.ACTION_EDITOR_ESCAPE, MyEscapeEditorActionHandler(actionManager.getActionHandler(IdeActions.ACTION_EDITOR_ESCAPE)))
            }

            private val supportedActions = listOf(
                    IdeActions.ACTION_EDITOR_CUT,
                    IdeActions.ACTION_EDITOR_PASTE,
                    IdeActions.ACTION_EDITOR_PASTE_SIMPLE,
                    IdeActions.ACTION_EDITOR_DELETE,
                    IdeActions.ACTION_EDITOR_DELETE_TO_WORD_START,
                    IdeActions.ACTION_EDITOR_DELETE_TO_WORD_END,
                    IdeActions.ACTION_EDITOR_DELETE_LINE,
                    IdeActions.ACTION_EDITOR_ENTER,
                    IdeActions.ACTION_EDITOR_START_NEW_LINE,
                    IdeActions.ACTION_EDITOR_SPLIT,
                    IdeActions.ACTION_EDITOR_FORWARD_PARAGRAPH,
                    IdeActions.ACTION_EDITOR_BACKWARD_PARAGRAPH,
                    IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET,
                    IdeActions.ACTION_EDITOR_UNSELECT_WORD_AT_CARET,
                    IdeActions.ACTION_EDITOR_BACKSPACE,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_UP_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_SWAP_SELECTION_BOUNDARIES,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_UP,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_DOWN,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_UP_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_MOVE_CARET_PAGE_DOWN_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_NEXT_WORD,
                    IdeActions.ACTION_EDITOR_PREVIOUS_WORD,
                    IdeActions.ACTION_EDITOR_NEXT_WORD_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_PREVIOUS_WORD_WITH_SELECTION,
                    IdeActions.ACTION_EDITOR_TAB,
                    IdeActions.ACTION_EDITOR_JOIN_LINES,
                    IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT,
                    IdeActions.ACTION_EDITOR_CLONE_CARET_BELOW,
                    IdeActions.ACTION_EDITOR_CLONE_CARET_ABOVE,
                    IdeActions.ACTION_EDITOR_NEXT_PARAMETER,
                    IdeActions.ACTION_EDITOR_PREV_PARAMETER,
                    IdeActions.ACTION_EDITOR_NEXT_TEMPLATE_VARIABLE,
                    IdeActions.ACTION_EDITOR_PREVIOUS_TEMPLATE_VARIABLE,
                    IdeActions.ACTION_EDITOR_DUPLICATE,
                    IdeActions.ACTION_EDITOR_DUPLICATE_LINES
            )
        }
    }
}
