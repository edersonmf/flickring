package com.emf.flickring;

import java.util.Collection;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.w3c.dom.Element;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.REST;
import com.flickr4java.flickr.test.TestInterface;
import com.google.common.collect.Maps;

@Slf4j
public class Uploader {

  public static void main(final String[] args) throws FlickrException {
    Flickr f = new Flickr("b56c0ee4e3a9262f6da9b712c8ae7266", "a8f58bf14a674c17", new REST());
    TestInterface testInterface = f.getTestInterface();
    Map<String, String> p = Maps.newHashMap();
    Collection<Element> results = testInterface.echo(p);
    for (Element e : results) {
      log.info("{} = {}", e.getNodeName(), e.getNodeValue());
    }
  }

}
