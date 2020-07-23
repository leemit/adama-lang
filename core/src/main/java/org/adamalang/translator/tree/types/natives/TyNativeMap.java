/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.types.natives;

import java.util.ArrayList;
import java.util.function.Consumer;
import org.adamalang.translator.env.Environment;
import org.adamalang.translator.parser.token.Token;
import org.adamalang.translator.tree.common.DocumentPosition;
import org.adamalang.translator.tree.types.TyType;
import org.adamalang.translator.tree.types.natives.functions.FunctionOverloadInstance;
import org.adamalang.translator.tree.types.natives.functions.FunctionStyleJava;
import org.adamalang.translator.tree.types.traits.CanBeMapDomain;
import org.adamalang.translator.tree.types.traits.IsMap;
import org.adamalang.translator.tree.types.traits.assign.AssignmentViaSetter;
import org.adamalang.translator.tree.types.traits.details.DetailHasBridge;
import org.adamalang.translator.tree.types.traits.details.DetailNativeDeclarationIsNotStandard;
import org.adamalang.translator.tree.types.traits.details.DetailTypeHasMethods;

public class TyNativeMap extends TyType implements //
    AssignmentViaSetter, //
    DetailHasBridge, //
    DetailTypeHasMethods, //
    DetailNativeDeclarationIsNotStandard, //
    IsMap //
{
  public final Token closeThing;
  public final Token commaToken;
  public final TyType domainType;
  public final Token mapToken;
  public final Token openThing;
  public final TyType rangeType;

  public TyNativeMap(final Token mapToken, final Token openThing, final TyType domainType, final Token commaToken, final TyType rangeType, final Token closeThing) {
    this.mapToken = mapToken;
    this.openThing = openThing;
    this.domainType = domainType;
    this.commaToken = commaToken;
    this.rangeType = rangeType;
    this.closeThing = closeThing;
    ingest(mapToken);
    ingest(closeThing);
  }

  @Override
  public void emit(final Consumer<Token> yielder) {
    yielder.accept(mapToken);
    yielder.accept(openThing);
    domainType.emit(yielder);
    yielder.accept(commaToken);
    rangeType.emit(yielder);
    yielder.accept(closeThing);
  }

  @Override
  public String getAdamaType() {
    return "map<" + domainType.getAdamaType() + "," + rangeType.getAdamaType() + ">";
  }

  @Override
  public String getBridge(final Environment environment) {
    return String.format("NativeBridge.WRAP_MAP(%s, %s)", //
        ((DetailHasBridge) getDomainType(environment)).getBridge(environment), //
        ((DetailHasBridge) getRangeType(environment)).getBridge(environment));
  }

  @Override
  public TyType getDomainType(final Environment environment) {
    return environment.rules.Resolve(domainType, false);
  }

  @Override
  public String getJavaBoxType(final Environment environment) {
    final var dt = getDomainType(environment);
    final var rt = getRangeType(environment);
    if (dt != null && rt != null) {
      return "NtMap<" + dt.getJavaBoxType(environment) + "," + rt.getJavaBoxType(environment) + ">";
    } else {
      return "NtMap<?,?>";
    }
  }

  @Override
  public String getJavaConcreteType(final Environment environment) {
    return getJavaBoxType(environment);
  }

  @Override
  public String getPatternWhenValueProvided(final Environment environment) {
    return "new " + getJavaBoxType(environment) + "(%s)";
  }

  @Override
  public TyType getRangeType(final Environment environment) {
    return environment.rules.Resolve(rangeType, false);
  }

  @Override
  public String getStringWhenValueNotProvided(final Environment environment) {
    return "new " + getJavaBoxType(environment) + "()";
  }

  @Override
  public TyNativeFunctional lookupMethod(final String name, final Environment environment) {
    if ("insert".equals(name)) {
      final var args = new ArrayList<TyType>();
      args.add(this);
      return new TyNativeFunctional("insert", FunctionOverloadInstance.WRAP(new FunctionOverloadInstance("insert", this, new ArrayList<>(args), true)), FunctionStyleJava.ExpressionThenArgs);
    }
    if ("size".equals(name)) {
      return new TyNativeFunctional("size", FunctionOverloadInstance.WRAP(new FunctionOverloadInstance("size", new TyNativeInteger(mapToken).withPosition(this), new ArrayList<>(), true)), FunctionStyleJava.ExpressionThenArgs);
    }
    return null;
  }

  @Override
  public TyType makeCopyWithNewPosition(final DocumentPosition position) {
    return new TyNativeMap(mapToken, openThing, domainType, commaToken, rangeType, closeThing).withPosition(position);
  }

  @Override
  public void typing(final Environment environment) {
    domainType.typing(environment);
    rangeType.typing(environment);
    final var resolvedDomainType = environment.rules.Resolve(domainType, false);
    if (resolvedDomainType != null && !(resolvedDomainType instanceof CanBeMapDomain)) {
      environment.document.createError(this, String.format("The domain type '%s' is not an appropriate.", resolvedDomainType.getAdamaType()), "TyNativeMap");
    }
  }
}