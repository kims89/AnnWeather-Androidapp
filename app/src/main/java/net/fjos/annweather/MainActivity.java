package net.fjos.annweather;

import android.content.Intent;
import android.location.Location;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;


import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.MaterialMultiAutoCompleteTextView;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.config.LocationParams;

public class MainActivity extends AppCompatActivity {

    final XPath xpath = XPathFactory.newInstance().newXPath();
    DocumentBuilder docBuilder = null;
    String land;
    String fylke;
    String kommune;
    String plass;
    Button GPSknapp;
    Button sokeknapp;
    MaterialAutoCompleteTextView sokefelt;
    List<String> stedshistorikk;
    StrictMode.ThreadPolicy policyInternet = new StrictMode.ThreadPolicy.Builder().permitAll().build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Her blir knappene og feltet knyttet opp.
        sokeknapp = (Button) findViewById(R.id.findButton);
        //Valgte å brukere MaterialTextView som gir et langt penere utseende. Det er også lagt på en mer forklarende
        //text når plass/sted blir skrevet inn i textviewet.
        //Grunnen til AutoComplete textview er for å kunne vise stedshistorikk ved å trykke på textviewet.
        sokefelt = (MaterialAutoCompleteTextView) findViewById(R.id.EditTextField);
        GPSknapp = (Button) findViewById(R.id.GPSbutton);

        //ny arraylist blir opprettet til historikken.
        stedshistorikk=new ArrayList<String>();

        //her starter aktiviteten når søkeknappen blir trykket på.
        sokeknapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //hvis ingenting blir skrevet i søkefeltet, blir det returnert en melding at man må.
                if (sokefelt.getText().toString().trim().length() == 0) {
                    Toast.makeText(getApplicationContext(), "Fyll inn søkefeltet", Toast.LENGTH_SHORT).show();
                } else {
                    //... hvis ikke blir det kjørt mot metoden "placewithname", stedet oppgitt av bruker vil bli kjørt gjennom UTF-8 encoding slik at
                    // den er klar for å brukes i URL og den vil klare å parse riktig XML.
                    try {
                        StrictMode.setThreadPolicy(policyInternet);
                        String Stedsnavn = URLEncoder.encode(sokefelt.getText().toString(), "UTF-8");
                        Placewithname(Stedsnavn);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
        GPSknapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finnGPS();
            }
        });


    }


    private void add() {
        //denne metoden oversetter og legger stedsnavnene som er søkt på i historikk.
// TODO Auto-generated method stub
        ArrayAdapter<String> adp=new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line,stedshistorikk);
        adp.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        sokefelt.setThreshold(1);
        sokefelt.setAdapter(adp);
    }


    private void setupDocumentBuilder() {
        docBuilder = null;
        if (docBuilder != null) {
            return;
        }

        // Setter opp Javas DOM XML parser
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            docBuilder = docBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Toast.makeText(getApplicationContext(), "Kan ikke hente data", Toast.LENGTH_SHORT).show();
            return;
        }
    }
    private Object getXPathValue(Document xmlDocument, String xPathExpression, QName returnType) {

        //Evaluerer og returnerer XPath-uttrykket mot parset XML. Metodene som bruker
        //til å parse i XMLen.
        XPathExpression expr;
        try {
            expr = xpath.compile(xPathExpression);
            return expr.evaluate(xmlDocument, returnType);
        } catch (XPathExpressionException e) {
            Toast.makeText(getApplicationContext(), "Skjedde en feil", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
    private String getXPathStringValue(Document xmlDocument, String xPathExpression) {
        return (String) getXPathValue(xmlDocument, xPathExpression, XPathConstants.STRING);
    }

    private NodeList getXPathNodeList(Document xmlDocument, String xPathExpression) {
        return (NodeList) getXPathValue(xmlDocument, xPathExpression, XPathConstants.NODESET);
    }

    public void findGPSname(String lat, String lon) {
        //findGPSname: URL til XML legges i requestUrlString ned lat og long fra GPS
        String requestUrlString = "http://api.geonames.org/findNearbyPostalCodes?lat="+lat+"&lng="+lon+"&maxRows=1&username=kimplaces";

        //XML parsing: XMLen blir parset til, deretter blir kommunen hentet ut. Hadde problemer med at YR ikke taklet flere av stedsnavnene fra Geonames
        setupDocumentBuilder();

        // Her lastes XMLen ned, om den feiler vil en beskje dukke opp hos brukeren (Dvs om Geonames ikke klarer å finne lokasjonen).
        Document doc;
        try {
            doc = docBuilder.parse(requestUrlString);
        } catch (SAXException |IOException e) {
            Toast.makeText(getApplicationContext(), "Fant ikke plassen, prøv igjen", Toast.LENGTH_SHORT).show();
            return;
        }

        //Her hentes kommunen brukeren befinner seg hentet ut.
        plass = getXPathStringValue(doc, "/geonames/code/adminCode2");

        //Her stopper GPS bruken.
        SmartLocation.with(this).location().stop();

        //Her kalles meoden som skal finne sted,fylke,land opp. Grunnen til at jeg bruker denne er for at koordinat tjenesten til Geonames ikke klarer å hente dette ut.
        Placewithname(plass);
    }


    public void Placewithname(String stedsnavn) {

        //Fant ut at enkelte stedsplasser kan ha mellomrom, her la jeg inn en replace i Stringen som bytter ut "+" med "%20"
        String sted = stedsnavn.replace("+", "%20");

        //Her legges URLen til i en string.
        String requestUrlString ="http://api.geonames.org/search?q=" +sted +"&maxRows=1&country=no&style=full&username=kimplaces" ;

        //Her startes en ny parsing mot Geonmaes.
        setupDocumentBuilder();

        //På samme måte som i GPS-meoden for uthenting av kommune blir det hentet XML fra Geonames tjeneste. Den vil
        //også skrive ut en feilmelding om det skulle vise seg at den ikke finner plassen.
        Document doc;
        try {
            doc = docBuilder.parse(requestUrlString );
        } catch (SAXException |IOException e ) {
            Toast.makeText(getApplicationContext(), "Noe skjedde galt, prøv igjen", Toast.LENGTH_SHORT).show();
            return;
        }
        //Her hentes fylke, kommune, plass og land ut av XMLen.
        fylke = getXPathStringValue(doc, "geonames/geoname/adminName1");
        kommune = getXPathStringValue(doc, "geonames/geoname/adminName2");
        plass = getXPathStringValue(doc, "geonames/geoname/name");
        land = getXPathStringValue(doc, "geonames/geoname/countryName");
        //Plassen legges inn i stedshistorikken, slik at dette også kan finnes tilbake til.
        stedshistorikk.add(plass);
        add();

        //Hvis plass ikke inneholder noe, vil den returnere en feilmelding. Hvis ikke vil den sende data videre
        //til WeatherActivity
        if(plass != null && !plass.isEmpty()) {
            //Har også lagt inn en liten replace da jeg fant ut at Geonames hadde lagt inn " fylke" bak fylkets navn. F.eks Finnmark Fylke
            String fylket= fylke.replace( " Fylke", "" );
            //Enkelte plasser inneholder airport. Dette fungerer ikke med YR. Dermed blir det tatt vekk.
            String plassen= plass.replace( " Airport", "" );
            Intent weatherIntent = new Intent(this, WeatherActivity.class);
            weatherIntent.putExtra("fylke", fylket);
            weatherIntent.putExtra("land", land);
            weatherIntent.putExtra("plass", plassen);
            weatherIntent.putExtra("kommune", kommune);
            startActivity(weatherIntent);
        }

        else {
            Toast.makeText(getApplicationContext(), "Fant ikke plassen, prøv igjen", Toast.LENGTH_SHORT).show();
            return;
        }


    }


    public void finnGPS(){
        //Her hentes GPSdataen ut, den kaller deretter opp en ny metode som behandler latitude og longitude.

        //Her vil tidligere lokasjon bli hentet om GPS-tjenesten ikke er på. Det skrives ut en melding til brukeren om dette
        if(SmartLocation.with(this).location().state().locationServicesEnabled()==false){
            Toast.makeText(getApplicationContext(), "Lokasjons-tjenesten er avslått. Henter sist lokasjon om mulig. Prøv å slå på lokasjon for nøyaktig" +
                    " GPS oppslag", Toast.LENGTH_LONG).show();
            Location lastLocation = SmartLocation.with(this).location().getLastLocation();
            if (lastLocation != null) {
                //Tillattelse for bruk av internett
                StrictMode.setThreadPolicy(policyInternet);
                //findGPSname metode metoden starter med latitude og longitude som varibler
                findGPSname(Double.toString(lastLocation.getLatitude()), Double.toString(lastLocation.getLongitude()));
            }
            //.. Om dette ikke er mulig blir denne meldingen skrevet ut.
            else {
                Toast.makeText(getApplicationContext(), "Ikke mulig å hente siste lokasjon, Slå på GPS", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            //smartlocation henter ut alt av lokasjonsdata
            SmartLocation.with(this).location().config(LocationParams.NAVIGATION).oneFix().start(new OnLocationUpdatedListener() {
                @Override

                public void onLocationUpdated(Location location) {
                    //latitude og longitude legges i to stringer.
                    String Latitude = Double.toString(location.getLatitude());
                    String Longitude = Double.toString(location.getLongitude());
                    //GPS: lokasjon hentet
                    //Tillattelse for bruk av internett
                    StrictMode.setThreadPolicy(policyInternet);
                    //findGPSname metode metoden starter med latitude og longitude som varibler
                    findGPSname(Latitude, Longitude);

                }
            });
        }

    }
}
