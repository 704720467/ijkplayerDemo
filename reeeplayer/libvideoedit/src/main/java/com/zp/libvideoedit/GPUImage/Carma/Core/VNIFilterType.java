package com.zp.libvideoedit.GPUImage.Carma.Core;

import java.util.ArrayList;

/**
 * Created by why8222 on 2016/2/25.
 */
public enum VNIFilterType {
    NONE,
    SPANN,
    SODA ,
    SATINY,
    POSTCARD,
    REED ,
    EARTH,
    RAIN,
    PRELUDE,
    NEON,
    ARCTIC ,
    AUTUMN,
    LAKE,
    COZY,
    DUSK,
    LCELAND,
    YOUTH,
    BARLEY,
    COLA,
    NOIR,
    LAVENDER,
    SIESTA,
    BANGKOK;
    public static String lookupMapping(VNIFilterType type){
        ArrayList<String> lookups = new ArrayList<String>();
        lookups.add("");
        lookups.add("lookup/Spann.png");
        lookups.add("lookup/Soda.png");
        lookups.add("lookup/Satiny.png");
        lookups.add("lookup/Postcard.png");
        lookups.add("lookup/Reed.png");
        lookups.add("lookup/Earth.png");
        lookups.add("lookup/Rain.png");
        lookups.add("lookup/Prelude.png");
        lookups.add("lookup/Neon.png");
        lookups.add("lookup/Arctic.png");
        lookups.add("lookup/Autumn.png");
        lookups.add("lookup/Lake.png");
        lookups.add("lookup/Cozy.png");
        lookups.add("lookup/Dusk.png");
        lookups.add("lookup/Iceland.png");
        lookups.add("lookup/Youth.png");
        lookups.add("lookup/Barley.png");
        lookups.add("lookup/Cola.png");
        lookups.add("lookup/Noir.png");
        lookups.add("lookup/Lavender.png");
        lookups.add("lookup/Siesta.png");
        lookups.add("lookup/Bangkok.png");
        int index =  VNIFilterType.allType().indexOf(type);
        return lookups.get(index);
    }

    public static ArrayList<VNIFilterType> allType() {
        ArrayList<VNIFilterType> typeList = new ArrayList<VNIFilterType>();
        typeList.add(NONE);
        typeList.add(SPANN);
        typeList.add(SODA);
        typeList.add(SATINY);
        typeList.add(POSTCARD);
        typeList.add(REED);
        typeList.add(EARTH);
        typeList.add(RAIN);
        typeList.add(PRELUDE);
        typeList.add(NEON);
        typeList.add(ARCTIC);
        typeList.add(AUTUMN);
        typeList.add(LAKE);
        typeList.add(COZY);
        typeList.add(DUSK);
        typeList.add(LCELAND);
        typeList.add(YOUTH);
        typeList.add(BARLEY);
        typeList.add(COLA);
        typeList.add(NOIR);
        typeList.add(LAVENDER);
        typeList.add(SIESTA);
        typeList.add(BANGKOK);
        return typeList;
    }

    public static VNIFilterType objectAt(int index){
        return VNIFilterType.allType().get(index);
    }
    public static int objectOfIndex(VNIFilterType type){
        return VNIFilterType.allType().indexOf(type);
    }

}
