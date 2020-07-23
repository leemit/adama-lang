/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.runtime.reactives;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;
import org.adamalang.runtime.contracts.CanGetAndSet;
import org.adamalang.runtime.contracts.RxChild;
import org.adamalang.runtime.contracts.RxParent;
import org.adamalang.runtime.natives.NtMaybe;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** a reactive maybe */
public class RxMaybe<Ty extends RxBase> extends RxBase implements RxParent, RxChild {
  private final Function<RxParent, Ty> maker;
  private Ty priorValue;
  private final ArrayList<RxChild> subscribers;
  private Ty value;

  public RxMaybe(final RxParent owner, final Function<RxParent, Ty> maker) {
    super(owner);
    this.subscribers = null;
    this.value = null;
    this.maker = maker;
    this.priorValue = null;
  }

  @Override
  public void __commit(final String name, final ObjectNode delta) {
    if (__isDirty()) {
      priorValue = value;
      if (value != null) {
        value.__commit(name, delta);
      } else { // value is null
        delta.putNull(name);
        __cancelAllSubscriptions();
      }
      __lowerDirtyCommit();
    }
  }

  @Override
  public boolean __raiseInvalid() {
    __invalidateSubscribers();
    return true;
  }

  @Override
  public void __revert() {
    if (__isDirty()) {
      value = priorValue;
      if (value != null) {
        value.__revert();
      } else { // value is null
        __cancelAllSubscriptions();
      }
      __lowerDirtyRevert();
    }
  }

  public int compareValues(final RxMaybe<Ty> other, final Comparator<Ty> test) {
    if (value == null) {
      if (other.value == null) {
        return 0;
      } else {
        return 1;
      }
    } else {
      if (other.value == null) {
        return -1;
      } else {
        return test.compare(value, other.value);
      }
    }
  }

  public void delete() {
    if (value != null) {
      value = null;
      __raiseDirty();
    }
  }

  public NtMaybe get() {
    if (value == null) {
      return new NtMaybe();
    } else {
      if (make() instanceof CanGetAndSet) {
        return new NtMaybe(((CanGetAndSet) value).get()).withDeleteChain(this);
      } else {
        return new NtMaybe(value).withDeleteChain(this);
      }
    }
  }

  public boolean has() {
    return value != null;
  }

  public Ty make() {
    if (value == null) {
      value = maker.apply(this);
      value.__subscribe(this);
      value.__raiseDirty();
    }
    return value;
  }

  public void set(final NtMaybe other) {
    if (other.has()) {
      ((CanGetAndSet) this.make()).set(other.get());
    } else {
      delete();
    }
    __raiseDirty();
  }
}