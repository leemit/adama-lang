/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.netty.server;

import org.adamalang.netty.api.GameSpaceDB;
import org.adamalang.netty.contracts.Authenticator;
import org.adamalang.netty.contracts.JsonHandler;
import org.adamalang.netty.contracts.ServerOptions;
import org.adamalang.netty.contracts.StaticSite;

public class ServerNexus {
  public final Authenticator authenticator;
  public final GameSpaceDB db;
  public final JsonHandler handler;
  public final ServerOptions options;
  public final StaticSite site;

  public ServerNexus(final ServerOptions options, final GameSpaceDB db, final JsonHandler handler, final Authenticator authenticator, final StaticSite site) {
    this.options = options;
    this.db = db;
    this.handler = handler;
    this.authenticator = authenticator;
    this.site = site;
  }

  public void shutdown() throws Exception {
    handler.shutdown();
    db.close();
  }
}
