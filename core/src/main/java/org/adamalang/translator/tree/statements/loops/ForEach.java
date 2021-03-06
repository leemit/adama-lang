/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.statements.loops;

import java.util.function.Consumer;
import org.adamalang.translator.env.ComputeContext;
import org.adamalang.translator.env.Environment;
import org.adamalang.translator.parser.token.Token;
import org.adamalang.translator.tree.common.StringBuilderWithTabs;
import org.adamalang.translator.tree.expressions.Expression;
import org.adamalang.translator.tree.statements.Block;
import org.adamalang.translator.tree.statements.ControlFlow;
import org.adamalang.translator.tree.statements.Statement;
import org.adamalang.translator.tree.types.TyType;
import org.adamalang.translator.tree.types.traits.details.DetailContainsAnEmbeddedType;

/** a modern and safe foreach(V in EXPR) code */
public class ForEach extends Statement {
  public final Block code;
  private TyType elementType;
  public final Token endParen;
  public final Token foreachToken;
  public final Token inToken;
  public final Expression iterable;
  public final Token openParen;
  public final String variable;
  public final Token variableToken;

  public ForEach(final Token foreachToken, final Token openParen, final Token variableToken, final Token inToken, final Expression iterable, final Token endParen, final Block code) {
    this.foreachToken = foreachToken;
    this.openParen = openParen;
    variable = variableToken.text;
    this.variableToken = variableToken;
    this.inToken = inToken;
    this.iterable = iterable;
    this.endParen = endParen;
    this.code = code;
    elementType = null;
    ingest(foreachToken);
    ingest(iterable);
    ingest(code);
  }

  @Override
  public void emit(final Consumer<Token> yielder) {
    yielder.accept(foreachToken);
    yielder.accept(openParen);
    yielder.accept(variableToken);
    yielder.accept(inToken);
    iterable.emit(yielder);
    yielder.accept(endParen);
    code.emit(yielder);
  }

  @Override
  public ControlFlow typing(final Environment environment) {
    final var type = iterable.typing(environment.scopeWithComputeContext(ComputeContext.Computation), null /* we know nothing to suggest */);
    if (type != null && environment.rules.IsIterable(type, false)) {
      elementType = ((DetailContainsAnEmbeddedType) type).getEmbeddedType(environment);
      if (elementType != null) {
        final var next = environment.scopeWithComputeContext(ComputeContext.Computation);
        next.define(variable, elementType, false, elementType);
        code.typing(next);
      }
    }
    return ControlFlow.Open;
  }

  @Override
  public void writeJava(final StringBuilderWithTabs sb, final Environment environment) {
    if (elementType != null) {
      sb.append("for(").append(elementType.getJavaBoxType(environment)).append(" ").append(variable).append(" : ");
      iterable.writeJava(sb, environment.scopeWithComputeContext(ComputeContext.Computation));
      sb.append(") ");
      final var next = environment.scopeWithComputeContext(ComputeContext.Computation);
      next.define(variable, elementType, false, elementType);
      code.writeJava(sb, next);
    }
  }
}
