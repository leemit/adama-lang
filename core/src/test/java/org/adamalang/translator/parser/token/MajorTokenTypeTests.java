/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.translator.parser.token;

import org.junit.Assert;
import org.junit.Test;

public class MajorTokenTypeTests {
  @Test
  public void coverage() {
    for (final MajorTokenType mtt : MajorTokenType.values()) {
      Assert.assertEquals(mtt, MajorTokenType.valueOf(mtt.name()));
    }
  }
}
