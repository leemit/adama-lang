/* The Adama Programming Language For Board Games!
 *    See http://www.adama-lang.org/ for more information.
 * (c) copyright 2020 Jeffrey M. Barber (http://jeffrey.io) */
package org.adamalang.runtime.json;

import java.util.ArrayDeque;
import org.adamalang.runtime.json.token.JsonToken;
import org.adamalang.runtime.json.token.JsonTokenType;
import org.adamalang.runtime.natives.NtClient;

public class JsonStreamReader {
  private int index;
  private final String json;
  private final int n;
  ArrayDeque<JsonToken> tokens;

  public JsonStreamReader(final String json) {
    this.json = json;
    n = json.length();
    tokens = new ArrayDeque<>();
  }

  public boolean end() {
    return index >= n;
  }

  private void ensureQueueHappy(final int needs) {
    if (tokens.size() > needs) { return; }
    while (tokens.size() < 10 + needs) {
      if (index < n) {
        readToken();
      } else {
        if (tokens.size() < needs) { throw new RuntimeException("Unable to satisfy minimum limit"); }
        return;
      }
    }
  }

  public String fieldName() {
    ensureQueueHappy(1);
    return tokens.removeFirst().data;
  }

  public boolean notEndOfArray() {
    ensureQueueHappy(1);
    final var first = tokens.peekFirst();
    if (first.type == JsonTokenType.EndArray) {
      tokens.removeFirst();
      return false;
    }
    return true;
  }

  public boolean notEndOfObject() {
    ensureQueueHappy(1);
    final var first = tokens.peekFirst();
    if (first.type == JsonTokenType.EndObject) {
      tokens.removeFirst();
      return false;
    }
    return true;
  }

  public boolean readBoolean() {
    ensureQueueHappy(1);
    return tokens.removeFirst().type == JsonTokenType.True;
  }

  public double readDouble() {
    ensureQueueHappy(1);
    return Double.parseDouble(tokens.removeFirst().data);
  }

  public int readInteger() {
    ensureQueueHappy(1);
    return Integer.parseInt(tokens.removeFirst().data);
  }

  public long readLong() {
    ensureQueueHappy(1);
    return Long.parseLong(tokens.removeFirst().data);
  }

  public NtClient readNtClient() {
    var agent = "?";
    var authority = "?";
    if (startObject()) {
      while (notEndOfObject()) {
        switch (fieldName()) {
          case "agent":
            agent = readString();
            break;
          case "authority":
            authority = readString();
            break;
        }
      }
    }
    return new NtClient(agent, authority);
  }

  public String readString() {
    ensureQueueHappy(1);
    return tokens.removeFirst().data;
  }

  private void readToken() {
    final var start = json.charAt(index);
    switch (start) {
      case '{':
        index++;
        tokens.addLast(new JsonToken(JsonTokenType.StartObject, null));
        return;
      case '}':
        index++;
        tokens.addLast(new JsonToken(JsonTokenType.EndObject, null));
        return;
      case '[':
        index++;
        tokens.addLast(new JsonToken(JsonTokenType.StartArray, null));
        return;
      case ']':
        index++;
        tokens.addLast(new JsonToken(JsonTokenType.EndArray, null));
        return;
      case ',':
      case ':':
        index++;
        readToken();
        return;
      case '\"':
        StringBuilder sb = null;
        for (var j = index + 1; j < n; j++) {
          var ch = json.charAt(j);
          if (ch == '\\') {
            if (sb == null) {
              sb = new StringBuilder();
              sb.append(json, index + 1, j);
            }
            j++;
            ch = json.charAt(j);
            switch (ch) {
              case 'n':
                sb.append('\n');
                break;
              case 't':
                sb.append('\t');
                break;
              case 'r':
                sb.append('\r');
                break;
              case 'f':
                sb.append('\f');
                break;
              case 'b':
                sb.append('\b');
                break;
              case '\\':
                sb.append('\\');
                break;
              case '"':
                sb.append('\"');
                break;
              case 'u':
                sb.append(Character.toString(Integer.parseInt(json.substring(j + 1, j + 5), 16)));
                j += 4;
            }
          } else if (ch == '"') {
            if (sb != null) {
              tokens.addLast(new JsonToken(JsonTokenType.StringLiteral, sb.toString()));
            } else {
              tokens.addLast(new JsonToken(JsonTokenType.StringLiteral, json.substring(index + 1, j)));
            }
            index = j + 1;
            return;
          } else {
            if (sb != null) {
              sb.append(ch);
            }
          }
        }
        throw new UnsupportedOperationException();
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
      case '-':
      case '+':
        for (var j = index + 1; j < n; j++) {
          final var ch2 = json.charAt(j);
          switch (ch2) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            case '-':
            case '+':
            case 'E':
            case 'e':
            case '.':
              break;
            default:
              tokens.addLast(new JsonToken(JsonTokenType.NumberLiteral, json.substring(index, j)));
              index = j;
              return;
          }
        }
        tokens.addLast(new JsonToken(JsonTokenType.NumberLiteral, json.substring(index)));
        index = n;
        return;
      case 'n':
        index += 4;
        tokens.addLast(new JsonToken(JsonTokenType.Null, null));
        return;
      case 't':
        index += 4;
        tokens.addLast(new JsonToken(JsonTokenType.True, null));
        return;
      case 'f':
        index += 5;
        tokens.addLast(new JsonToken(JsonTokenType.False, null));
        return;
      default:
        throw new UnsupportedOperationException();
    }
  }

  public void skipValue() {
    if (startObject()) {
      while (notEndOfObject()) {
        fieldName();
        skipValue();
      }
    } else if (startArray()) {
      while (notEndOfArray()) {
        skipValue();
      }
    } else {
      ensureQueueHappy(1);
      tokens.removeFirst();
    }
  }

  public void skipValue(final JsonStreamWriter writer) {
    if (startObject()) {
      writer.beginObject();
      while (notEndOfObject()) {
        writer.writeObjectFieldIntro(fieldName());
        skipValue(writer);
      }
      writer.endObject();
    } else if (startArray()) {
      writer.beginArray();
      while (notEndOfArray()) {
        skipValue(writer);
      }
      writer.endArray();
    } else {
      ensureQueueHappy(1);
      final var token = tokens.removeFirst();
      if (token.type == JsonTokenType.NumberLiteral) {
        writer.injectJson(token.data);
      } else if (token.type == JsonTokenType.StringLiteral) {
        writer.writeString(token.data);
      } else if (token.type == JsonTokenType.Null) {
        writer.writeNull();
      } else if (token.type == JsonTokenType.True) {
        writer.writeBoolean(true);
      } else if (token.type == JsonTokenType.False) {
        writer.writeBoolean(false);
      }
    }
  }

  public boolean startArray() {
    ensureQueueHappy(1);
    final var first = tokens.peekFirst();
    if (first.type == JsonTokenType.StartArray) {
      tokens.removeFirst();
      return true;
    }
    return false;
  }

  public boolean startObject() {
    ensureQueueHappy(1);
    final var first = tokens.peekFirst();
    if (first.type == JsonTokenType.StartObject) {
      tokens.removeFirst();
      return true;
    }
    return false;
  }

  public boolean testLackOfNull() {
    ensureQueueHappy(1);
    final var first = tokens.peekFirst();
    if (first.type == JsonTokenType.Null) {
      tokens.removeFirst();
      return false;
    }
    return true;
  }
}
