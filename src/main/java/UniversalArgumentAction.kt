import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.template.impl.editorActions.TypedActionHandlerBase
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorAction
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

class UniversalArgumentAction : EditorAction(Handler()) {

    internal class Handler : EditorActionHandler() {
        override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext?) {
            if (!ourActionsRegistered) {
                val actionManager = EditorActionManager.getInstance()

                val typedAction = actionManager.typedAction
                typedAction.setupRawHandler(MyTypedHandler(typedAction.rawHandler))

                for (action in supportedActions) {
                    actionManager.setActionHandler(action, MyEditorActionHandler(actionManager.getActionHandler(action)))
                }
                ourActionsRegistered = true
            }
            count = 0
            isEnabled = true
        }

        class MyTypedHandler(originalHandler: TypedActionHandler?) : TypedActionHandlerBase(originalHandler) {
            override fun execute(editor: Editor, charTyped: Char, dataContext: DataContext) {
                if (isEnabled) doUniversalArgument(charTyped, editor, dataContext)
                else myOriginalHandler?.execute(editor, charTyped, dataContext)
            }

            private fun doUniversalArgument(charTyped: Char, editor: Editor, dataContext: DataContext) {
                if (charTyped.isDigit()) {
                    count = charTyped.toString().toInt() + count * 10
                    HintManager.getInstance().showInformationHint(editor, "$count")
                }
                else repeatUniversalArgument { myOriginalHandler?.execute(editor, charTyped, dataContext) }

            }
        }

        class MyEditorActionHandler(private val myOriginalHandler: EditorActionHandler) : EditorActionHandler() {
            public override fun doExecute(editor: Editor, caret: Caret?, dataContext: DataContext) {
                if (isEnabled) repeatUniversalArgument { myOriginalHandler.execute(editor, caret, dataContext) }
                else myOriginalHandler.execute(editor, caret, dataContext)
            }

            override fun isEnabledForCaret(editor: Editor, caret: Caret, dataContext: DataContext?): Boolean =
                    isEnabled || myOriginalHandler.isEnabled(editor, caret, dataContext)
        }


        companion object {
            private var ourActionsRegistered = false
            private var isEnabled = false
            private var count = 0
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

            private fun repeatUniversalArgument(action: (kotlin.Int) -> kotlin.Unit) {
                isEnabled = false
                if (count == 0) count = 4
                repeat(count) { action.invoke(it) }
                count = 0
            }
        }
    }
}
