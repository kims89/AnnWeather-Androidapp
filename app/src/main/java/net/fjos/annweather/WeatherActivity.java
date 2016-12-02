package net.fjos.annweather;


import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class WeatherActivity extends AppCompatActivity {

    private ListView listView1;
    String Test;
    final String serviceBaseURL = "http://www.yr.no/sted/";
    final XPath xpath = XPathFactory.newInstance().newXPath();
    DocumentBuilder docBuilder = null;
    ArrayList<Weather> weatherlist = new ArrayList<Weather>();
    TextView stedNaa;
    TextView graderNaa;
    TextView vindNaa;
    TextView msgToday;
    String Land;
    String Fylke;
    String Plass;
    String Kommune;
    StrictMode.ThreadPolicy policyInternet = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        msgToday = (TextView)findViewById(R.id.infoMsgToday);
        graderNaa = (TextView)findViewById(R.id.tempNowtextView);
        vindNaa = (TextView)findViewById(R.id.vindNowtextView);

        //data blir hentet fra MainActivity (fylke, kommune,plass og land)
        Intent intent = getIntent();
        Land = intent.getStringExtra("land");
        Plass = intent.getStringExtra("plass");
        Kommune = intent.getStringExtra("kommune");
        Fylke = intent.getStringExtra("fylke");
        //stedsnavet settes med engang.
        stedNaa = (TextView)findViewById(R.id.plassNowtextView);
        stedNaa.setText(Plass+", "+Fylke);

        StrictMode.setThreadPolicy(policyInternet);
        //Dataen fra MainActivity sendes videre til Forecast-metoden som tar seg av parsing av XML til Yr.
        Forecast(Land,Fylke,Kommune,Plass);

        //Listview settes opp. Her settes også opp mot en ny layout-fil som brukes på hver av objektene i XMLen.
        WeatherAdapter adapter = new WeatherAdapter(this,
                R.layout.listview_item_row, weatherlist);

        listView1 = (ListView)findViewById(R.id.listView1);
        View header = (View)getLayoutInflater().inflate(R.layout.listview_header_row, null);
        listView1.addHeaderView(header);
        listView1.setAdapter(adapter);
    }

    //På samme måte som i MainActivity settes det opp en Setupdocumentbuilder til parsingen av XMLen til YR.
    private void setupDocumentBuilder() {
        if (docBuilder != null) {
            return;
        }

        // Setter opp Javas DOM XML parser
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Toast.makeText(getApplicationContext(), "Kan ikke hente data", Toast.LENGTH_SHORT).show();
        }
    }

    private Object getXPathValue(Document xmlDocument, String xPathExpression, QName returnType) {

        // Evaluerer og returnerer XPath-uttrykket mot parset XML
        XPathExpression expr;
        try {
            expr = xpath.compile(xPathExpression);
            return expr.evaluate(xmlDocument, returnType);
        } catch (XPathExpressionException e) {
            Toast.makeText(getApplicationContext(), "Skjedde noe feil", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private String getXPathStringValue(Document xmlDocument, String xPathExpression) {
        return (String) getXPathValue(xmlDocument, xPathExpression, XPathConstants.STRING);
    }

    private NodeList getXPathNodeList(Document xmlDocument, String xPathExpression) {
        return (NodeList) getXPathValue(xmlDocument, xPathExpression, XPathConstants.NODESET);
    }


    public void Forecast(String land, String fylke, String kommune, String plass) {
        //Stedsnavn, fylke, kommune og kommune blir satt gjennom encoding, hvor bla mellomrom blir byttet ut med "%20"
            String land1 = java.net.URLEncoder. encode(land).replace("+", "%20");
            String fylke1 = java.net.URLEncoder. encode(fylke).replace("+", "%20");
            String kommune1 = java.net.URLEncoder. encode(kommune).replace("+", "%20");
            String plass1 = java.net.URLEncoder. encode(plass).replace("+", "%20");
        String requestUrlString = serviceBaseURL+"/"+land1+"/"+fylke1+"/"+kommune1+"/"+plass1+"/"+"forecast.xml";

        // Sett opp docBuilder om den ikke allerede er satt opp
        setupDocumentBuilder();

        // Laster ned og parser XML
        Document doc;
        try {
            doc = docBuilder.parse(requestUrlString);
        } catch (SAXException |IOException e) {
            Toast.makeText(getApplicationContext(), "Klarer ikke finne været", Toast.LENGTH_SHORT).show();
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
            return;
        }

        // Skriv ut temperatur akkurat nå (første timesvarsel)
        String infoNaaRaw = getXPathStringValue(doc, "/weatherdata/forecast/text/location/time[1]/body");
        String temperature = getXPathStringValue(doc, "/weatherdata/observations/weatherstation[1]/temperature/@value");
        String unit = getXPathStringValue(doc, "/weatherdata/observations/weatherstation[1]/temperature/@unit");
        String vindspeed = getXPathStringValue(doc, "/weatherdata/observations/weatherstation[1]/windSpeed/@name");
        String winddirection = getXPathStringValue(doc, "/weatherdata/observations/weatherstation[1]/windDirection/@name");


        //Værmelding i tekst legges i været i dag. Fjerner <strong> og </strong> så det skal se bra ut.
        String infoNaa = infoNaaRaw.replace("<strong>", "").replace("</strong>","");
        //resten av været idag settes her.
        msgToday.setText(infoNaa);
        graderNaa.setText(temperature + "° " +unit);
        vindNaa.setText("Vindstyrken er "+vindspeed+" fra retning "+winddirection+".");

        // Skriv ut temperaturer for det neste døgnet:
        //Været blir lagt i en for-løkke, her blir alle dataene lagt i et objekt. Det er også en teller som teller værmeldingen, deretter blir det hentet ned på tur.
        int numForeCasts = getXPathNodeList(doc, "/weatherdata/forecast/tabular/time").getLength();
        //teller starter på 1, den blir deretter økt når et objekt er ferdig. Dette gjør den til den har kjørt gjennom hele værmeldingen i XMLen.
        for (int teller = 1; teller <= numForeCasts; teller++) {
            temperature =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/temperature/@value");
            unit =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/temperature/@unit");
            String fromTime =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/@from");
            String toTime =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/@to");
            String windDirection =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/windDirection/@name");
            String windSpeed =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/windSpeed/@name");
            String icons =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/symbol/@var");
            String sky =
                    getXPathStringValue(doc, "/weatherdata/forecast/tabular/time[" + teller + "]/symbol/@name");

            //datoen og tiden formateres.
            DateTime dtFrom = new DateTime(fromTime);
            DateTime.Property mOYf = dtFrom.monthOfYear();
            String finalFromTime=dtFrom.getHourOfDay()+":00 "+ dtFrom.getDayOfMonth()+". "+mOYf.getAsShortText()+" "+dtFrom.getYear();

            DateTime dtTo = new DateTime(toTime);
            DateTime.Property mOYt = dtFrom.monthOfYear();
            String finalToTime=dtTo.getHourOfDay()+":00 "+ dtTo.getDayOfMonth()+". "+mOYt.getAsShortText()+" "+dtTo.getYear();

            //værikonene til YR er satt opp slik at spesial tegn blir lagt under en mappe kalt mf. Problemet med Android (Studio) er
            //mapper ikke støttes under drawable, dette løste jeg med å ha alle filene flatt i katalogen, og fjerne "mf/" fra ikonverdien fra XML-filen. Android støtter heller ikke
            //tall som første tegn i bildenavnet, dette løste jeg med å rename alle filene til "b..."
            if(icons.indexOf("mf/")!=-1){
                String fixedicons = icons.replaceAll("[mf/]","").replaceAll("[.]", "");
                weatherlist.add(new Weather("b" + fixedicons,sky,"vindretning "+windDirection+" Vindstyrke "+windSpeed,temperature+"° "+
                        unit,finalFromTime+" - "+finalToTime));
            }
            else{
                weatherlist.add(new Weather("b" + icons,sky,"vindretning "+windDirection+" Vindstyrke "+windSpeed,temperature+"° "+
                        unit,"fra "+finalFromTime+" til "+finalToTime));
            }



        }
    }
}
