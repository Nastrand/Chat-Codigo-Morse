package com.ucab.javachat.Servidor.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ucab.javachat.Servidor.model.EnvioCorreo;
import com.ucab.javachat.Servidor.controller.ServidorController;

/**
 *
 * @author Administrador
 */
public class ServidorModel extends Thread
{
     Socket scli=null;
     Socket scli2=null;
     DataInputStream entrada=null;
     DataOutputStream salida=null;
     DataOutputStream salida2=null;
     public static Vector<ServidorModel> clientesActivos = new Vector<ServidorModel>();	
     private String nameUser;
     private String clave;
     private File imagen;
     ServidorController serv;
     ArrayList<Usuario> usuariosArchivo = new ArrayList<Usuario>();
     
     public ServidorModel(Socket scliente,Socket scliente2,ServidorController serv)
     {
        scli=scliente;
        scli2=scliente2;
        this.serv=serv;
        clientesActivos.add(this);        
        serv.mostrar("cliente agregado: "+this);			
     }
     
     public void setNameUser(String user)
     {
       this.nameUser = user;
     }
     
     public String getNameUser() {
    	 return nameUser;
     }
     
     public void setClave(String clave)
     {
       this.clave = clave;
     }
     
     public String getClave() {
    	 return clave;
     }
     
	public File getImagen() {
		return imagen;
	}

	public void setImagen(File imagen) {
		this.imagen = imagen;
	}

	public void run()
     {
		Gson gson = new Gson();
    	serv.mostrar(".::Esperando Mensajes :");
    	byte[] sizeAr;
    	int size;
    	byte[] imageAr;
    	BufferedImage image;
    	File dir;
    	try
    	{
          entrada = new DataInputStream(scli.getInputStream());
          salida = new DataOutputStream(scli.getOutputStream());
          salida2 = new DataOutputStream(scli2.getOutputStream());
          int caseInicio = 0;
          caseInicio = entrada.readInt();
          switch(caseInicio) {
          	case 1: // Inicio de sesion
          		this.setNameUser(entrada.readUTF());
          		this.setClave(entrada.readUTF());
          		
          		dir = new File("." + "/Documentos/verificacionDe" + getNameUser() + ".jpg");
                sizeAr = new byte[4];
                entrada.read(sizeAr);
                size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
                imageAr = new byte[size];
                entrada.read(imageAr);
                image = ImageIO.read(new ByteArrayInputStream(imageAr));
                ImageIO.write(image, "jpg", new File(dir.getCanonicalPath()));
                this.imagen = dir.getCanonicalFile();
          		
          		
          		Autenticacion inicioDeSesion = new Autenticacion(this.getNameUser(), this.getClave(), this.getImagen());
          		// Envia true o false si el inicio de sesion es valido o invalido
          		//boolean flagInicioSesion = inicioDeSesion.autenticar();
          		Usuario autenticado = inicioDeSesion.autenticar();
          		if (autenticado != null) {
	          		String autenticadoJson =  gson.toJson(autenticado);
	                salida.writeUTF(autenticadoJson);
	                serv.mostrar("Ha iniciado sesion: "+this.getNameUser());
	                
          		} else {
          			salida.writeUTF("Fallo");
          			scli.close();
                    scli2.close();
          		}
          		if (this.imagen.delete()) serv.mostrar("Se ha eliminado la imagen de verificaion.");
          		break;
          	case 2: // Registro
                String usuarioRegistroJson = entrada.readUTF();
                Usuario usuarioRegistro = gson.fromJson(usuarioRegistroJson, new TypeToken<Usuario>() {}.getType());
               
                File miDir = new File ("." + "/Documentos/Imagenes de Verificacion/" +
                        usuarioRegistro.getNombreDeUsuario() + ".jpg");
                sizeAr = new byte[4];
                entrada.read(sizeAr);
                size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
                imageAr = new byte[size];
                entrada.read(imageAr);
                image = ImageIO.read(new ByteArrayInputStream(imageAr));
                ImageIO.write(image, "jpg", new File(miDir.getCanonicalPath()));
                usuarioRegistro.setImagen(miDir.getCanonicalFile());
                Autenticacion registro = new Autenticacion(usuarioRegistro); 
                boolean flagRegistro = registro.registrar();
                salida.writeBoolean(flagRegistro);
                if (flagRegistro) {
                    serv.mostrar("Nuevo usuario registrado");
                }
                scli.close();
                scli2.close();
                break;
          	case 3: //Recuperar contraseña 
          		String correo = entrada.readUTF();
          		dir = new File("/Documentos/qweiqwueklasriqwkehqwgeertsdffitgfog.jpg");
          		
          		sizeAr = new byte[4];
                entrada.read(sizeAr);
                size = ByteBuffer.wrap(sizeAr).asIntBuffer().get();
                imageAr = new byte[size];
                entrada.read(imageAr);
                image = ImageIO.read(new ByteArrayInputStream(imageAr));
                ImageIO.write(image, "jpg", new File(dir.getCanonicalPath()));
                this.imagen = dir.getCanonicalFile();
				try {
					correo = Criptologia.desencriptar(correo);
				} catch (Exception e) {
					e.printStackTrace();
				}
          		Autenticacion recupera = new Autenticacion(correo, this.imagen);
          		String clave = recupera.comparaContraseña();
          		if (clave != null) {
	          		EnvioCorreo envio = new EnvioCorreo(correo, "Recuperación de contraseña", "La clave es:  "+clave);
					envio.enviar();
					salida.writeBoolean(true);
          		} else {
          			salida.writeBoolean(false);
          		}
          		if (this.imagen.delete()) serv.mostrar("Se ha eliminado la imagen de verificaion.");
          		scli.close();
          		scli2.close();
	          	break;
          }
    	}
    	catch (IOException e) {  
    		e.printStackTrace();     
    	}
    	
        int opcion=0,numUsers=0;
        String mencli = "";
        String amigostring = "";
        Vector<String> amigos = new Vector<String>();
                
    	while(true){
          try{
             opcion = entrada.readInt();
             switch(opcion)
             {
                case 1: // Modificar datos
                	String usuarioRegistroJson = entrada.readUTF();
                    Usuario	usuarioRegistro=gson.fromJson(usuarioRegistroJson, new TypeToken<Usuario>() {}.getType());
                    String nombreInicial = entrada.readUTF();
                	Autenticacion modificarUsuario = new Autenticacion(usuarioRegistro, nombreInicial); 
                	boolean flagRegistro = modificarUsuario.modificar();
                	salida.writeBoolean(flagRegistro);
                	
                    if (flagRegistro) {
                        System.out.println("guardado");
                    }
                    else 
                    	serv.mostrar("no se pudo");
                   break;
                case 2://envio de lista de activos
                   numUsers = clientesActivos.size();
                   salida.writeInt(numUsers);
                   for(int i=0;i<numUsers;i++)
                      salida.writeUTF(clientesActivos.get(i).nameUser);
                   break;
                case 3: // envia mensaje privado
                    mencli = entrada.readUTF();//mensaje enviado
                    String emisor = entrada.readUTF();
                    amigostring = entrada.readUTF();
                    amigos = gson.fromJson(amigostring, new TypeToken<Vector<String>>() {}.getType()); 
                    enviaMensaje(mencli, emisor, amigos, amigostring);
                   	break;
	             }
          }
          	catch (IOException e) {System.out.println("El cliente termino la conexion.");break;}
    	}
    	serv.mostrar("Se removio un usuario.");
    	clientesActivos.removeElement(this);
    	try{
    		serv.mostrar("Se desconecto un usuario.");
    		scli.close();
    	}	
        catch(Exception et){
        	serv.mostrar("No se puede cerrar el socket.");
        	}   
     }
    
     public void enviaUserActivos()
     {
        ServidorModel user=null;
        for(int i=0;i<clientesActivos.size();i++)
        {           
           try
            {
              user=clientesActivos.get(i);
              if(user==this)continue; //ya se lo envie
              user.salida2.writeInt(2); //opcion de agregar 
              user.salida2.writeUTF(this.getNameUser());	
            }catch (IOException e) {e.printStackTrace();}
        }
     }
   
   private void enviaMensaje(String mencli, String emisor, Vector<String> amigos, String jsonamigos) 
   {
      ServidorModel user=null;
      for (String amigo : amigos) {
        for(int i = 0; i < clientesActivos.size(); i++)
        {           
           try
            {
              user = clientesActivos.get(i);
              if(user.nameUser.equals(amigo))
              {
                 user.salida2.writeInt(3);//opcion de mensaje amigo
                 user.salida2.writeUTF(mencli);
                 user.salida2.writeUTF(emisor);
                 user.salida2.writeUTF(jsonamigos);
              }
            }catch (IOException e) {e.printStackTrace();}
        }
      }
   }
}