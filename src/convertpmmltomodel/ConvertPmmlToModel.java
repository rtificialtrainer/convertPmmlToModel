package convertpmmltomodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.xml.transform.Source;
import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.Model;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Target;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.ModelEvaluator;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.model.ImportFilter;
import org.jpmml.model.JAXBUtil;
import org.jpmml.model.visitors.LocatorNullifier;
import org.xml.sax.InputSource;

/**
 *
 * @author DELL
 */
public class ConvertPmmlToModel {
    
    static Integer mode;
    static Scanner input;
    
    public static void main(String[] args) throws Exception {

        input = new Scanner(System.in);
        String format = null;
        File pmmlFile;
        String filePath;
        
        System.out.println("\n=====PMML Converter - Mateusz Tumidajewicz=====");
                
        while(true){
            System.out.print("\nEnter PMML file path: ");
            filePath = input.nextLine();
            if(filePath.length()==0||!filePath.endsWith(".xml")){
                System.out.println("\nYou have not entered PMML file path.");
                System.out.println("If you want to combine several files into one");
                System.out.println("name each file: model_name-nr.xml for example model-01.xml");
                System.out.println("and run program as for a single file");
                System.out.println("and enter first model path as a parameter.");
            }else{
                pmmlFile = new File(filePath);
                if(!pmmlFile.exists()){
                    System.out.println("\nFile does not exist\n");
                }else break;
            }
        }
        
        OUTER:
        while (true) {
            System.out.println("\nSelect mode:");
            System.out.println("1. Training generation");
            System.out.println("2. Result prediction");
            System.out.print("Mode: ");
            switch (input.next()) {
                case "1":
                    format = ".gtm";
                    mode = 1;
                    break OUTER;
                case "2":
                    format = ".prm";
                    mode = 2;
                    break OUTER;
                default:
                    System.out.println("\nWrong type!");
                    break;
            }
        }
            
        String serFile = filePath.replace(".xml",format);
        if(filePath.endsWith("-01.xml")){
            
            String pmmlFileName = pmmlFile.getName();
            final String filter = pmmlFileName.substring(0, pmmlFileName.lastIndexOf("-")+1);
            
            File[] pmmlFiles = pmmlFile.getAbsoluteFile().getParentFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {                   
                        return name.endsWith(".xml")&&
                                name.startsWith(filter)&&
                                name.substring(name.lastIndexOf("-")+1, name.lastIndexOf(".")).matches("[0-9]+");
                }
            });
                    
            if(pmmlFiles.length>1){
                System.out.println("\nRelated models:");
                for(File xmlfile : pmmlFiles){
                    System.out.println(xmlfile.getName());
                }
                
                System.out.print("\nCombine models into one?[Y/N]: ");
                if(input.next().toUpperCase().contentEquals("Y")){
                    serFile = serFile.replace("-01","");
                    convertPmmlToSer(pmmlFiles,new File(serFile));
                } else{
                    convertPmmlToSer(new File[]{pmmlFile},new File(serFile));
                }
            }else convertPmmlToSer(new File[]{pmmlFile},new File(serFile));
        }else{
            convertPmmlToSer(new File[]{pmmlFile},new File(serFile));
        }
        System.out.println("\nGenerated model: "+serFile);
    }
    public static void convertPmmlToSer(File[] pmmlArray,File serFile) throws Exception {
        
        List<PMML>pmmlList = new ArrayList<>();	
        PMML pmml;

        for(File pmmlFile : pmmlArray){
            try(InputStream is = new FileInputStream(pmmlFile)){
                Source source = ImportFilter.apply(new InputSource(is));
		pmml = JAXBUtil.unmarshalPMML(source);
            }
            // Remove SAX Locator information
            LocatorNullifier locatorNullifier = new LocatorNullifier();
            locatorNullifier.applyTo(pmml);
            
            pmmlList.add(pmml);
        }
        
        Map<String,List> pmmlMap = new HashMap<>();
        pmmlMap.put("model", pmmlList);
        //jeżeli GT
        if(mode==1){
            System.out.print("\nDefine charts for model outputs?[Y/N]: ");
            if(input.next().toUpperCase().contentEquals("Y"))pmmlMap.put("chart", createChartDef(pmmlList));
        }
        
        try(OutputStream os = new FileOutputStream(serFile)){
      	//SerializationUtil.serializePMML(pmml, os);
            serializePmmlMap(pmmlMap,os);
	}
    }
    static private void serializePmmlMap(Map<String,List> object, OutputStream os) throws IOException {
		FilterOutputStream safeOs = new FilterOutputStream(os){

			@Override
			public void close() throws IOException {
				super.flush();
			}
		};

		try(ObjectOutputStream oos = new ObjectOutputStream(safeOs)){
			oos.writeObject(object);

			oos.flush();
		}
	}
    
    private static List<GTChart> createChartDef(List<PMML> pmmlList){
        
        Integer index = 1;
        Map<Integer,DataField> outputs = new HashMap<>();
        
        BufferedReader in = new BufferedReader(System.console().reader());

        for(PMML pmml : pmmlList){
            ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
            Evaluator evaluator = modelEvaluatorFactory.newModelManager(pmml);
            evaluator.verify();
            
            for(FieldName fieldName : evaluator.getTargetFields()){
                outputs.put(index, evaluator.getDataField(fieldName));
                index++;
            }
        }
        
        System.out.println("Output fields:");
        for(Integer key : outputs.keySet()){
            System.out.println(key+". "+outputs.get(key).getDisplayName());
        }
        
        System.out.print("\nEnter number of charts: ");
        Integer chartCount = Integer.parseInt(input.next());
        List<GTChart> wykresyList = new ArrayList<>();
        
        for(int i=1;i<=chartCount;i++){
            System.out.print("\nEnter name for chart nr "+i+": ");
            String nazwa="";
            try{
                nazwa=in.readLine();
            }catch(Exception e){
                
            }
            GTChart wykres = new GTChart(nazwa);
            
            Map<String, String> entriesMap;
            while (true) {
                entriesMap = new LinkedHashMap<>();
                System.out.println("\nEnter numbers of outputs fields (separated by ,) for chart nr "+i+".");
                System.out.println("Output fields:");
                for(Integer key : outputs.keySet()){
                    System.out.println(key+". "+outputs.get(key).getDisplayName());
                }
                System.out.print("\nPola: ");
                String[] entries = input.next().split(",");
                if(entries.length>0){
                    for(int j=0;j<entries.length;j++){
                        try{
                            Integer numer = Integer.parseInt(entries[j]);
                            if(outputs.containsKey(numer))entriesMap.put(outputs.get(numer).getName().toString().replace("Predicted_", ""), outputs.get(numer).getDisplayName());
                            else {
                                System.out.println("\nNo output with nr "+numer);
                                break;
                            }
                        }catch (Exception e){
                            System.out.println("\nEntered value must be integer");
                            break;
                        }
                    }
                    if(entries.length==entriesMap.size())break;
                    else System.out.println("\nYou enetered several times one value of field");
                }else System.out.println("\nYou did not enter values of fields");
            }
            
            wykres.setEntries(entriesMap);
            
            wykresyList.add(wykres);
        }
        
        return wykresyList;
    }
}
