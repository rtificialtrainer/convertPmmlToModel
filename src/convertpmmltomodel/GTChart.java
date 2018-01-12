/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package convertpmmltomodel;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author DELL
 */
public class GTChart implements Serializable {
    
    private String name;
    private Map<String,String> entries;

    public GTChart(String name) {
        this.name = name;
        entries = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Map<String,String> getEntries(){
        return entries;
    }
    
    public void setEntries(Map<String,String> entries){
        this.entries=entries;
    }
    
    public void addEntry(String key, String entry){
        this.entries.put(key, entry);
    }
}
