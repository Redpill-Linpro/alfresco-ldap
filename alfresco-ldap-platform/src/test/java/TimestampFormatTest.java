import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Ignore;
import org.junit.Test;



public class TimestampFormatTest {

  DateFormat timestampFormat;
  
  public static final String THEDATE = "20151012064810.757Z";
  
  @Ignore
  @Test
  public void testOpenLdap() throws ParseException {
    setTimestampFormat("yyyyMMddHHmmss'Z'");
    timestampFormat.parse(THEDATE);
  }
  
  @Ignore
  @Test
  public void activeDirectory() throws ParseException {
    setTimestampFormat("yyyyMMddHHmmss'.0Z'");
    timestampFormat.parse(THEDATE);
  }
  
  @Ignore
  @Test
  public void other() throws ParseException {
    setTimestampFormat("yyyyMMddHHmmss");
    timestampFormat.parse(THEDATE);
  }
  
  /**
   * Sets the timestamp format. Unfortunately, this varies between directory servers.
   * 
   * @param timestampFormat
   *            the timestamp format
   *            <ul>
   *            <li>OpenLDAP: "yyyyMMddHHmmss'Z'"
   *            <li>Active Directory: "yyyyMMddHHmmss'.0Z'"
   *            </ul>
   */
  public void setTimestampFormat(String timestampFormat)
  {
      this.timestampFormat = new SimpleDateFormat(timestampFormat, Locale.UK);
      this.timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }
}
