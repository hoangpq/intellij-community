package com.intellij.compiler;

import com.intellij.compiler.server.BuildManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.intellij.util.io.TestFileSystemItem.fs;

/**
 * @author nik
 */
public class CorruptedBuildCachesTest extends BaseCompilerTestCase {

  public void testTimestampsStorage() {
    VirtualFile a = createFile("src/A.java", "class A{}");
    Module m = addModule("m", a.getParent());
    make(m);
    assertOutput(m, fs().file("A.class"));
    File systemDirectory = BuildManager.getInstance().getProjectSystemDirectory(myProject);

    assertTrue(FileUtil.delete(new File(systemDirectory, "timestamps")));
    changeFile(a, "class A{int b;}");
    make(m).assertGenerated("A.class");
    make(m).assertUpToDate();

    File file = new File(systemDirectory, "timestamps/data");
    corruptCaches(file);
    changeFile(a, "class A{int c;}");
    make(m).assertGenerated("A.class");
    make(m).assertUpToDate();
  }

  public void testSrcOutMapping() {
    VirtualFile a = createFile("src/A.java", "class A{}");
    Module m = addModule("m", a.getParent());
    make(m);
    assertOutput(m, fs().file("A.class"));
    File systemDirectory = BuildManager.getInstance().getProjectSystemDirectory(myProject);

    assertTrue(FileUtil.delete(new File(systemDirectory, "targets/java-production/m/src-out")));
    changeFile(a, "class A{int b;}");
    make(m).assertGenerated("A.class");
    make(m).assertUpToDate();

    changeFile(a, "class A{int c;}");
    corruptCaches(new File(systemDirectory, "targets/java-production/m/src-out/data"));
    make(m).assertGenerated("A.class");

    assertOutput(m, fs().file("A.class"));
  }

  private static void corruptCaches(File file) {
    try {
      DataOutputStream stream = new DataOutputStream(new FileOutputStream(file.getAbsolutePath()));
      stream.writeInt(-1);
      stream.close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
