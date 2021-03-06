/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.types.natives;

import java.util.function.Consumer;
import org.adamalang.translator.env.Environment;
import org.adamalang.translator.parser.token.Token;
import org.adamalang.translator.tree.common.DocumentPosition;
import org.adamalang.translator.tree.types.TyType;
import org.adamalang.translator.tree.types.TypeBehavior;
import org.adamalang.translator.tree.types.traits.CanBeNativeArray;
import org.adamalang.translator.tree.types.traits.details.DetailRequiresResolveCall;

public class TyNativeRef extends TyType implements //
    CanBeNativeArray, //
    DetailRequiresResolveCall //
{
  public final Token readonlyToken;
  public final String ref;
  public final Token refToken;

  public TyNativeRef(final TypeBehavior behavior, final Token readonlyToken, final Token refToken) {
    super(behavior);
    this.readonlyToken = readonlyToken;
    this.refToken = refToken;
    ref = refToken.text;
    ingest(refToken);
  }

  @Override
  public void emit(final Consumer<Token> yielder) {
    if (readonlyToken != null) {
      yielder.accept(readonlyToken);
    }
    yielder.accept(refToken);
  }

  @Override
  public String getAdamaType() {
    return ref;
  }

  @Override
  public String getJavaBoxType(final Environment environment) {
    throw new UnsupportedOperationException("the reference must be resolved");
  }

  @Override
  public String getJavaConcreteType(final Environment environment) {
    throw new UnsupportedOperationException("the reference must be resolved");
  }

  @Override
  public TyType makeCopyWithNewPosition(final DocumentPosition position, final TypeBehavior newBehavior) {
    return new TyNativeRef(newBehavior, readonlyToken, refToken).withPosition(position);
  }

  @Override
  public TyType resolve(final Environment environment) {
    final var other = environment.document.types.get(ref);
    if (other != null) { return other.makeCopyWithNewPosition(other, behavior); }
    return null;
  }

  @Override
  public void typing(final Environment environment) {
    // handled by environment.rules.Revolve
  }
}
