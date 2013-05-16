package org.purl.wf4ever.robundle.fs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestFileSystemProvider {

    @Test
    public void getInstance() throws Exception {
        assertSame(BundleFileSystemProvider.getInstance(), BundleFileSystemProvider.getInstance());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getInstanceEquals() throws Exception {
        assertEquals(BundleFileSystemProvider.getInstance(),
                new BundleFileSystemProvider());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void getInstanceHashCode() throws Exception {
        assertEquals(BundleFileSystemProvider.getInstance().hashCode(),
                new BundleFileSystemProvider().hashCode());
    }
    
    @SuppressWarnings({ "deprecation", "static-access" })
    @Test
    public void sameOpen() throws Exception {
        assertSame(BundleFileSystemProvider.getInstance().openFilesystems,
                new BundleFileSystemProvider().openFilesystems);
    }
    
	@Test
	public void installedProviders() throws Exception {
		for (FileSystemProvider provider : FileSystemProvider
				.installedProviders()) {
			if (provider instanceof BundleFileSystemProvider) {
				assertSame(provider, BundleFileSystemProvider.getInstance());
				return;
			}
		}
		fail("Could not find BundleFileSystemProvider as installed provider");
	}

	@Test
	public void newByURI() throws Exception {

		Path path = Files.createTempFile("test", "zip");
		BundleFileSystemProvider.createBundleAsZip(path, null);

		// HACK: Use a opaque version of widget: with the file URI as scheme
		// specific part
		URI w = new URI("widget", path.toUri().toASCIIString(), null);
		FileSystem fs = FileSystems.newFileSystem(w,
				Collections.<String, Object> emptyMap());
		assertTrue(fs instanceof BundleFileSystem);
	}

	@Test
    public void jarWithSpaces() throws Exception {
        Path path = Files.createTempFile("with several spaces", ".zip");
        Files.delete(path);
        
        // Will fail with FileSystemNotFoundException without env:
        //FileSystems.newFileSystem(path, null);
        
        // Neither does this work, as it does not double-escape:
        // URI jar = URI.create("jar:" + path.toUri().toASCIIString());                

        URI jar = new URI("jar", path.toUri().toString(), null);
        assertTrue(jar.toASCIIString().contains("with%2520several%2520spaces"));
        
        Map<String, Object> env = new HashMap<>();
        env.put("create", "true");
 
        try (FileSystem fs = FileSystems.newFileSystem(jar, env)) {
            URI root = fs.getPath("/").toUri();    
            assertTrue(root.toString().contains("with%2520several%2520spaces"));
        } 
        // Reopen from now-existing Path to check that the URI is
        // escaped in the same way
        try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
            URI root = fs.getPath("/").toUri();
            //System.out.println(root.toASCIIString());
            assertTrue(root.toString().contains("with%2520several%2520spaces"));
        }
    }
	
	@Test
    public void jarWithUnicode() throws Exception {
	    Path path = Files.createTempFile("with\u2301unicode\u263bhere", ".zip");
        Files.delete(path);
        //System.out.println(path); // Should contain a electrical symbol and smiley
        URI jar = new URI("jar", path.toUri().toString(), null);
        //System.out.println(jar);
        assertTrue(jar.toString().contains("\u2301"));
        assertTrue(jar.toString().contains("\u263b"));        
        
        Map<String, Object> env = new HashMap<>();
        env.put("create", "true");
 
        try (FileSystem fs = FileSystems.newFileSystem(jar, env)) {            
            URI root = fs.getPath("/").toUri();
            //System.out.println(root.toASCIIString());
            assertTrue(root.toString().contains("\u2301"));
        }
        
        // Reopen from now-existing Path to check that the URI is
        // escaped in the same way
        try (FileSystem fs = FileSystems.newFileSystem(path, null)) {
            URI root = fs.getPath("/").toUri();
            //System.out.println(root.toASCIIString());
            assertTrue(root.toString().contains("\u2301"));
        }
    }
	
	@Test
	public void newFileSystemFromExisting() throws Exception {
		Path path = Files.createTempFile("test", null);
		Files.delete(path);
		BundleFileSystemProvider.createBundleAsZip(path, "application/x-test");
		assertTrue(Files.exists(path));
		BundleFileSystem f = BundleFileSystemProvider.newFileSystemFromExisting(path);
		assertEquals(path, f.getSource());
		assertEquals("application/x-test", Files.readAllLines(
				f.getRootDirectory().resolve("mimetype"), 
				Charset.forName("ASCII")).get(0));
	}
	
	@Test
	public void newFileSystemFromNewDefaultMime() throws Exception {
		Path path = Files.createTempFile("test", null);
		Files.delete(path);
		BundleFileSystem f = BundleFileSystemProvider.newFileSystemFromNew(path);
		assertTrue(Files.exists(path));
		assertEquals(path, f.getSource());
		assertEquals("application/vnd.wf4ever.robundle+zip", Files.readAllLines(
				f.getRootDirectory().resolve("mimetype"), 
				Charset.forName("ASCII")).get(0));
	}
	
	@Test
	public void newFileSystemFromNew() throws Exception {
		Path path = Files.createTempFile("test", null);
		Files.delete(path);
		BundleFileSystem f = BundleFileSystemProvider.newFileSystemFromNew(path, "application/x-test2");
		assertTrue(Files.exists(path));
		assertEquals(path, f.getSource());
		assertEquals("application/x-test2", Files.readAllLines(
				f.getRootDirectory().resolve("mimetype"), 
				Charset.forName("ASCII")).get(0));
	}
	
	@Test
	public void newFileSystemFromTemporary() throws Exception {
		BundleFileSystem f = BundleFileSystemProvider.newFileSystemFromTemporary();
		assertTrue(Files.exists(f.getSource()));
		assertEquals("application/vnd.wf4ever.robundle+zip", Files.readAllLines(
				f.getRootDirectory().resolve("mimetype"), 
				Charset.forName("ASCII")).get(0));
	}
	

}