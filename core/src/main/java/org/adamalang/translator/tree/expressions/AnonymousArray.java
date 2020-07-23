/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.expressions;

import java.util.ArrayList;
import java.util.function.Consumer;
import org.adamalang.translator.env.Environment;
import org.adamalang.translator.parser.token.Token;
import org.adamalang.translator.tree.common.TokenizedItem;
import org.adamalang.translator.tree.types.TyType;
import org.adamalang.translator.tree.types.checking.properties.StorageTweak;
import org.adamalang.translator.tree.types.natives.TyNativeArray;
import org.adamalang.translator.tree.types.natives.TyNativeMessage;
import org.adamalang.translator.tree.types.structures.StorageSpecialization;
import org.adamalang.translator.tree.types.structures.StructureStorage;
import org.adamalang.translator.tree.types.traits.SupportsTwoPhaseTyping;

/** an anonymous array of items [item1, item2, ..., itemN] */
public class AnonymousArray extends Expression {
  private static final TyNativeMessage EMPTY_MESSAGE = new TyNativeMessage(null, Token.WRAP("__EmptyMessageNoArgs_"), new StructureStorage(StorageSpecialization.Message, true, null));
  public Token closeBracketToken;
  public final ArrayList<TokenizedItem<Expression>> elements;
  public Token openBracketToken;

  public AnonymousArray(final Token openBracketToken) {
    elements = new ArrayList<>();
    this.openBracketToken = openBracketToken;
    ingest(openBracketToken);
  }

  /** add an anonymous object to the array */
  public void add(final TokenizedItem<Expression> aobject) {
    elements.add(aobject);
    ingest(aobject.item);
  }

  @Override
  public void emit(final Consumer<Token> yielder) {
    yielder.accept(openBracketToken);
    for (final TokenizedItem<Expression> element : elements) {
      element.emitBefore(yielder);
      element.item.emit(yielder);
      element.emitAfter(yielder);
    }
    yielder.accept(closeBracketToken);
  }

  public void end(final Token closeBracketToken) {
    this.closeBracketToken = closeBracketToken;
    ingest(closeBracketToken);
  }

  @Override
  protected TyType typingInternal(final Environment environment, final TyType suggestion) {
    if (suggestion != null) {
      if (environment.rules.IsNativeArray(suggestion, false)) {
        final var elementType = environment.rules.ExtractEmbeddedType(suggestion, false);
        for (final TokenizedItem<Expression> elementExpr : elements) {
          final var computedType = elementExpr.item.typing(environment, elementType);
          environment.rules.CanTypeAStoreTypeB(elementType, computedType, StorageTweak.None, false);
        }
        return suggestion;
      }
      return null;
    } else {
      TyType proposal = null;
      if (elements.size() > 0) {
        final var firstExpr = elements.get(0).item;
        if (firstExpr instanceof SupportsTwoPhaseTyping) {
          proposal = ((SupportsTwoPhaseTyping) firstExpr).estimateType(environment);
        } else {
          proposal = firstExpr.typing(environment, suggestion instanceof TyNativeArray ? environment.rules.ExtractEmbeddedType(suggestion, false) : null);
        }
      }
      if (proposal == null) {
        proposal = EMPTY_MESSAGE;
      }
      for (final TokenizedItem<Expression> elementExpr : elements) {
        TyType candidate = null;
        if (elementExpr.item instanceof SupportsTwoPhaseTyping) {
          candidate = ((SupportsTwoPhaseTyping) elementExpr.item).estimateType(environment);
        }
        if (candidate == null) {
          candidate = elementExpr.item.typing(environment, null);
        }
        if (candidate != null) {
          proposal = proposal == null ? candidate : environment.rules.GetMaxType(proposal, candidate, false);
        }
      }
      proposal = environment.rules.EnsureRegisteredAndDedupe(proposal, false);
      if (proposal != null) {
        for (final TokenizedItem<Expression> elementExpr : elements) {
          if (elementExpr.item instanceof SupportsTwoPhaseTyping) {
            ((SupportsTwoPhaseTyping) elementExpr.item).upgradeType(environment, proposal);
          }
        }
      }
      if (proposal != null) {
        return new TyNativeArray(proposal, null).withPosition(this);
      } else {
        return proposal;
      }
    }
  }

  @Override
  public void writeJava(final StringBuilder sb, final Environment environment) {
    environment.mustBeComputeContext(this);
    final var me = (TyNativeArray) cachedType;
    if (me != null) {
      sb.append("new ").append(me.getJavaConcreteType(environment)).append(" {");
      var first = true;
      for (final TokenizedItem<Expression> element : elements) {
        if (first) {
          first = false;
        } else {
          sb.append(", ");
        }
        element.item.writeJava(sb, environment);
      }
      sb.append("}");
    }
  }
}