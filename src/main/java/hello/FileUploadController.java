package hello;

import java.io.IOException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hello.storage.StorageFileNotFoundException;
import hello.storage.StorageService;
import java.io.File;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class FileUploadController {

    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {

        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) throws IOException {


        model.addAttribute("files", storageService.loadAll().map(
                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", path.getFileName().toString()).build().toString())
                .collect(Collectors.toList()));

        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {


        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

           
        storageService.store(file);
        try {
            System.out.println(file.getOriginalFilename());
            String name = file.getOriginalFilename();
            String cmd1 = "unrar x " + name + " ./" ;
            System.out.println(cmd1);
            
            command(cmd1, new File("./upload-dir/"),System.out);           
            
            name = name.substring(0,name.lastIndexOf("."));                      
            System.out.println(name);
            PrintStream out =new PrintStream( new File ("./upload-dir/" + name+".txt"));
            String cmd2 = "java -jar ../sond/Freq.jar ./"+name+" -bin";
            System.out.println(cmd2);
            command(cmd2, new File("./upload-dir/"),out);            
            out.close();
        } catch (InterruptedException ex) {
            Logger.getLogger(FileUploadController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(FileUploadController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         String x = new File ("./").getAbsolutePath() ;
        for (File F: new File("./upload-dir/").listFiles()){            
            x+= "\n" + F.getName()+"";
        };
        //executar script 
        redirectAttributes.addFlashAttribute("message",

          // "Diretório padrão: " + x );        
                "Seu arquivo " + file.getOriginalFilename() + " foi carregado com sucesso! " );
//        System.out.println(this.rootLocation.resolve(filename).toString());


        return "redirect:/";
    }  

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        

         
        return ResponseEntity.notFound().build();
    }
    public static void command(String cmd, File dir, final PrintStream out) throws IOException, InterruptedException, Exception{
        final Process comp = Runtime.getRuntime().exec(cmd, null, dir);
        final ArrayBlockingQueue<Boolean> control = new ArrayBlockingQueue<Boolean>(5);
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Scanner sc = new Scanner(comp.getInputStream());
                while(sc.hasNextLine()){
                    String s = sc.nextLine();
                    out.println(s);
                }
                sc.close();
                control.add(true);
            }
        });
        
        
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Scanner sc = new Scanner(comp.getErrorStream());
                
                while(sc.hasNextLine()){
                    String s = sc.nextLine();
                    out.println(s);
                }
                sc.close();
                control.add(true);
            }
        });
        
        comp.waitFor(); //wait the process
        for(int n=0; n<2; n++){
            control.take(); //wait the threads
        }
        comp.destroy();
        
        
    }

    private class rootLocation {

        public rootLocation() {
        }
    }
    
    
}
