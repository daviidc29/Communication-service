package co.edu.escuelaing.uplearn.chat;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;

public final class TestUtils {
  private static final ObjectMapper M = new ObjectMapper();
  private TestUtils() {}

  public static void setField(Object target, String field, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) { throw new RuntimeException(e); }
  }

  public static String toJson(Object o) {
    try { return M.writeValueAsString(o); }
    catch (Exception e) { throw new RuntimeException(e); }
  }

  public static String toJsonArray(List<?> l) {
    try { return M.writeValueAsString(l); }
    catch (Exception e) { throw new RuntimeException(e); }
  }

  public static String jwtWithSub(String sub) {
    String header = b64url("{\"alg\":\"none\"}");
    String payload = b64url("{\"sub\":\"" + sub + "\"}");
    return header + "." + payload + ".";
  }

  private static String b64url(String s) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes());
  }
}
