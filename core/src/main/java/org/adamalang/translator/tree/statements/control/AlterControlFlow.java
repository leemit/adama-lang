/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.statements.control;

import java.util.function.Consumer;
import org.adamalang.translator.env.Environment;
import org.adamalang.translator.parser.token.Token;
import org.adamalang.translator.tree.common.StringBuilderWithTabs;
import org.adamalang.translator.tree.statements.ControlFlow;
import org.adamalang.translator.tree.statements.Statement;

public class AlterControlFlow extends Statement {
  public final AlterControlFlowMode how;
  public final Token semicolonToken;
  public final Token token;

  public AlterControlFlow(final Token token, final AlterControlFlowMode how, final Token semicolonToken) {
    this.token = token;
    this.how = how;
    this.semicolonToken = semicolonToken;
    ingest(token);
    ingest(semicolonToken);
  }

  @Override
  public void emit(final Consumer<Token> yielder) {
    yielder.accept(token);
    yielder.accept(semicolonToken);
  }

  @Override
  public ControlFlow typing(final Environment environment) {
    if (how == AlterControlFlowMode.Abort && !environment.state.isMessageHandler()) {
      environment.document.createError(this, String.format("Can only 'abort' from a message handler"), "EvaluationContext");
    }
    if (how == AlterControlFlowMode.Block && !environment.state.isStateMachineTransition()) {
      environment.document.createError(this, String.format("Can only 'block' from a state machine transition"), "EvaluationContext");
    }
    if (how == AlterControlFlowMode.Abort || how == AlterControlFlowMode.Block) { return ControlFlow.Returns; }
    return ControlFlow.Open;
  }

  @Override
  public void writeJava(final StringBuilderWithTabs sb, final Environment environment) {
    if (how == AlterControlFlowMode.Break) {
      sb.append("break;");
    } else if (how == AlterControlFlowMode.Continue) {
      sb.append("continue;");
    } else if (how == AlterControlFlowMode.Abort) {
      sb.append("throw new AbortMessageException();");
    } else if (how == AlterControlFlowMode.Block) {
      sb.append("throw new ComputeBlockedException(null, null);");
    }
  }
}
