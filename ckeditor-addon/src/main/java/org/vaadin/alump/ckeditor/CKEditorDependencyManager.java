package org.vaadin.alump.ckeditor;

import com.vaadin.server.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * Klasse um alle benötigten Bibliotheken und Plugins des CKEditors bereitzustellen
 *
 * @author m.escher, 16.07.2020
 */
public class CKEditorDependencyManager
{
  private static final String ROOT_PATH = "/org/vaadin/alump/ckeditor/";
  private static final String BLACKLISTED_EXTENSION = ".class";
  private static final Set<String> BLACKLISTED_FILES = Stream.of("editor/ckeditor.js", "vaadin-save-plugin.js").collect(Collectors.toSet());

  /**
   * Registriert alle Dependencies die für den CKEditor notwendig sind
   */
  public static void registerDependencies()
  {
    LegacyCommunicationManager manager = VaadinSession.getCurrent().getCommunicationManager();
    Set<String> resources = _collectResources();
    resources.forEach(r -> {
      manager.registerDependency(r, CKEditorDependencyManager.class);
    });
  }

  /**
   * Liefert alle Resourcen aus einem normalen Ordner
   *
   * @param pFolder Ordner auf der Festplatte
   * @return Map
   */
  private static Set<String> _loadResourcesFromNormalFolder(File pFolder)
  {
    File[] children = pFolder.listFiles((dir, name) -> _shouldPublishFile(name));
    if (children == null)
      throw new IllegalStateException("Failed to load children from folder " + pFolder);

    // Hier können Kinder direkt aus dem Ordner ausgelesen werden
    return Arrays.stream(children).map(file -> _getRelativeFilename(file.getName())).collect(Collectors.toSet());
  }

  /**
   * Gibt den relativen Filename aus dem Absoluten zurück
   *
   * @param pAbsoluteFilename der absolute Filename
   * @return der relative Filename
   */
  private static String _getRelativeFilename(String pAbsoluteFilename)
  {
    return pAbsoluteFilename.replace(ROOT_PATH.substring(1), "");
  }

  /**
   * @return alle Resourcen, die zur Verfügung stehen
   */
  private static Set<String> _collectResources()
  {
    URL data = CKEditorDependencyManager.class.getResource(ROOT_PATH);
    if (data == null)
      return new HashSet<>();

    File dataFolder = new File(data.getPath());
    if (dataFolder.exists() && dataFolder.isDirectory())
      return _loadResourcesFromNormalFolder(dataFolder);
    else
      return _loadResourcesFromInnerJar(data);
  }

  /**
   * Liefert alle Resourcen aus einer jar Datei
   *
   * @param pJarFile URL zur Jar
   * @return Map
   */
  private static Set<String> _loadResourcesFromInnerJar(URL pJarFile)
  {
    // Daten können NICHT direkt aus dem Verzeichnis gelesen werden.
    // Beispielsweise wenn die XML-Dateien innerhalb einer JAR liegen
    String[] split = pJarFile.toExternalForm().split("!");
    if (split.length != 2)
      return _loadResourcesFromNormalFolder(new File(pJarFile.getPath()));

    String pathToJar = split[0];
    if (pathToJar.startsWith("jar:file:"))
      pathToJar = pathToJar.substring(9);

    String pathInJar = split[1];
    if (pathInJar.startsWith("/"))
      pathInJar = pathInJar.substring(1);

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(pathToJar)))
    {
      Set<String> result = new HashSet<>();
      for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry())
        if (!entry.isDirectory() && entry.getName().startsWith(pathInJar) && _shouldPublishFile(entry.getName()))
          result.add(_getRelativeFilename(entry.getName()));
      return result;
    }
    catch (Exception e)
    {
      throw new RuntimeException("Failed to load Resources from system folder", e);
    }
  }

  /**
   * Prüft ob die übergebene File vom ignoriert und sommit nicht veröffentlicht werden soll.
   *
   * @param pFilename der Dateiname
   * @return true wenn die Datei ingoriert werden soll
   */
  private static boolean _shouldPublishFile(String pFilename)
  {
    return !pFilename.endsWith(BLACKLISTED_EXTENSION) && !BLACKLISTED_FILES.contains(_getRelativeFilename(pFilename));
  }
}
