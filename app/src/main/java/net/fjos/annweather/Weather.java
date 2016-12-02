package net.fjos.annweather;

/**
 * Created by Kim on 01.04.2016.
 */

//værmeldingene blir lagt inn i objekter. Her blir ikoner, tittel, tid, vindstyrke og retning hentet ut.
public class Weather {
    public String icon;
    public String title;
    public String vind;
    public String temperatur;
    public String tid;


    public Weather(){
        super();
    }

    public Weather(String icon, String title, String vind, String temperatur, String tid) {
        super();
        this.icon = icon;
        this.title = title;
        this.vind = vind;
        this.temperatur = temperatur;
        this.tid = tid;
    }
}