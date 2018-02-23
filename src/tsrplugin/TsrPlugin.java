/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tsrplugin;
import devplugin.*;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Main class for this plugin
 * @author Pavion
 */
public class TsrPlugin extends devplugin.Plugin {
    private Properties mSettings;
    private Config ret; 

    private static final util.ui.Localizer mLocalizer
                    = util.ui.Localizer.getLocalizerFor( TsrPlugin.class );
    
    Locale[] supportedLocales = {
        Locale.GERMAN,
        Locale.ENGLISH
    };

    private static TsrPlugin mInstance;
    public TsrPlugin() {
        mInstance = this;    
    }
    
    protected static TsrPlugin getInstance() {
        if (mInstance == null) {
            mInstance = new TsrPlugin();
        }
        return mInstance;
    }
    
    @Override
    public Properties storeSettings() {
        return mSettings;
    }

    @Override
    public void loadSettings(Properties settings) {
        mSettings = settings;
    }

    public static Version getVersion() {        
        return new Version(1,1,0,true);
    }
    
    @Override
    public PluginInfo getInfo() {        
        String name = mLocalizer.msg( "pluginName" ,"TsrPlugin" );
        String desc = mLocalizer.msg( "pluginDesc" ,"Remote control for tvstreamrecord, a Synology recording package" );

        return new PluginInfo(TsrPlugin.class, name, desc, 
                "Pavion", "GPL", "http://pavion.github.io/tvstreamrecord/");
    }
    
    @Override
    public SettingsTab getSettingsTab() {              
        SettingsTab st = new SettingsTab() {            
            
            @Override
            public JPanel createSettingsPanel() {                
                ret = new Config();
                String urlMessage = mLocalizer.msg( "urlMessage" ,"TVStreamRecord URL and Port (e.g. http://0.0.0.0:8030)" );
                String url = mSettings.getProperty("URL");
                ret.setLabel(urlMessage);
                ret.setURL(url);
                return ret;
            }

            @Override
            public void saveSettings() {
                if (ret!=null) {
                    mSettings.setProperty("URL", ret.getURL());                
                }
            }

            @Override
            public Icon getIcon() {
                ImageIcon icon = createImageIcon("img/tvstreamrecord.png");
                return icon;
            }

            @Override
            public String getTitle() {
                return "TvStreamRecord plugin";
            }
        }; 
        return st;
    }
    
    @Override
    public ActionMenu getButtonAction() {
        AbstractAction action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(Desktop.isDesktopSupported())
                {
                    try {
                        Desktop.getDesktop().browse(new URI(mSettings.getProperty("URL")));
                    } catch (IOException ex) {
                        Logger.getLogger(TsrPlugin.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (URISyntaxException ex) {
                        Logger.getLogger(TsrPlugin.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        String name = mLocalizer.msg("menuOpen","Open TvStreamRecord");
        action.putValue(Action.NAME, name);        

        // Der Aktion ein kleines Icon geben. Dieses Icon wird im Menü gezeigt
        // Das Icon sollte 16x16 Pixel groß sein
        ImageIcon icon = createImageIcon("img/tvstreamrecord.png");
        action.putValue(Action.SMALL_ICON, icon);

        // Der Aktion ein großes Icon geben. Dieses Icon wird in der Symbolleiste gezeigt
        // Das Icon sollte 22x22 Pixel groß sein
        icon = createImageIcon("img/tvstreamrecord22.png");
        action.putValue(BIG_ICON, icon);

        // Das Aktions-Menü erzeugen und zurückgeben
        return new ActionMenu(action); 
    }
    
    @Override
    public ActionMenu getContextMenuActions(Program program) {        
        AbstractAction action = new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent evt) {
                sendAnTSR(program);
            }
        };

        // Der Aktion einen Namen geben. Dieser Name wird dann im Kontextmenü gezeigt
        String name = mLocalizer.msg("popupCaption","Capture with TvStreamRecord");
        action.putValue(Action.NAME, name);        

        // Der Aktion ein Icon geben. Dieses Icon wird mit dem Namen im Kontextmenü gezeigt
        // Das Icon sollte 16x16 Pixel groß sein
        ImageIcon icon = createImageIcon("img/tvstreamrecord.png");
        action.putValue(Action.SMALL_ICON, icon);

        // Das Aktions-Menü erzeugen und zurückgeben
        return new ActionMenu(action); 
    }

    protected void sendAnTSR(Program program) {
        try {                
            String url = mSettings.getProperty("URL") + "/createtvb";
            
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            
            con.setRequestMethod("POST");
            
            String recname = program.getTitle();
            String sender = program.getChannel().getName();            
            String von = program.getTimeString();
            String bis = program.getEndTimeString();
            
            Calendar cal = program.getDate().getCalendar();
            String am = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            
            String urlParameters = String.format("recname=%s&sender=%s&von=%s&bis=%s&am=%s", recname, sender, von, bis, am);
            
            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.write(urlParameters.getBytes("UTF-8"));
            wr.flush();
            wr.close();
            
            int responseCode = con.getResponseCode();
            //System.out.println("\nSending 'POST' request to URL : " + url);
            //System.out.println("Post parameters : " + urlParameters);
            //System.out.println("Response Code : " + responseCode);
            
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            
            program.mark(this);
            program.validateMarking();
            
            //System.out.println(response.toString());
        } catch (MalformedURLException ex) {
            Logger.getLogger(TsrPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException ex) {
            Logger.getLogger(TsrPlugin.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TsrPlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    @Override
    public int getMarkPriorityForProgram(Program p) {
        return Program.MEDIUM_MARK_PRIORITY;
    }
}
