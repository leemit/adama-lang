/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.netty.api;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.adamalang.runtime.contracts.TimeSource;
import org.adamalang.runtime.exceptions.ErrorCodeException;
import org.adamalang.translator.env.CompilerOptions;

/** a mapping of files to their game spaces */
public class GameSpaceDB {
  private static void sanityCheckDataDirectory(final File file) throws ErrorCodeException {
    if (!file.exists()) {
      file.mkdir();
    }
    if (!(file.exists() && file.isDirectory())) { throw new ErrorCodeException(ErrorCodeException.CONFIGURATION_MALFORMED_NO_SOURCE_DIRECTORY); }
  }

  private int classId;
  private final File dataRoot;
  private final HashMap<String, GameSpace> map;
  private final CompilerOptions options;
  private final File sourceRoot;
  private final TimeSource time;

  public GameSpaceDB(final File sourceRoot, final File dataRoot, final CompilerOptions options, final TimeSource time) throws Exception {
    this.sourceRoot = sourceRoot;
    this.options = options;
    if (!sourceRoot.exists()) { throw new Exception("Source root: `" + dataRoot.getName() + "` does not exist"); }
    sanityCheckDataDirectory(dataRoot);
    this.dataRoot = dataRoot;
    this.time = time;
    map = new HashMap<>();
    classId = 0;
  }

  public synchronized void close() throws Exception {
    for (final Map.Entry<String, GameSpace> entry : map.entrySet()) {
      entry.getValue().close();
    }
    map.clear();
  }

  /** get a gamespace (via filename) */
  public synchronized GameSpace getOrCreate(final String game) throws ErrorCodeException {
    var gs = map.get(game);
    if (gs != null) { return gs; }
    final var gameSource = new File(sourceRoot, game);
    if (!gameSource.exists()) { throw new ErrorCodeException(ErrorCodeException.USERLAND_CANT_FIND_GAMESPACE); }
    final var gameData = new File(dataRoot, game);
    sanityCheckDataDirectory(gameData);
    final var factory = GameSpace.buildLivingDocumentFactory(sourceRoot, options, game, "Game" + classId++);
    gs = new GameSpace(factory, time, gameData);
    map.put(game, gs);
    return gs;
  }
}
