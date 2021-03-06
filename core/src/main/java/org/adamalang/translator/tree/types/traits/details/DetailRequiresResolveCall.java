/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.tree.types.traits.details;

import org.adamalang.translator.env.Environment;
import org.adamalang.translator.tree.types.TyType;

public interface DetailRequiresResolveCall {
  TyType resolve(Environment environment);
}
