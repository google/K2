// Copyright 2014 Google. Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.k2crypto.i18n;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Locale;

/**
 * Unit tests for i18n Messages. 
 * 
 * @author darylseah@gmail.com (Daryl Seah)
 */
@RunWith(JUnit4.class)
public class MessagesTest {

  @Before public void setUp() {
    // We are testing with English messages
    Messages.changeLocale(new Locale("en"));    
  }
  
  /**
   * Tests getting a known message.
   */
  @Test public final void testGetMessage() {
    String msg = Messages.getString("misc.arg.null");
    assertEquals("Argument for \"{0}\" should not be null.", msg);
  }

  /**
   * Tests getting a known message with parameters.
   */
  @Test public final void testGetMessageParams() {
    String msg = Messages.getString("misc.arg.null", "key");
    assertEquals("Argument for \"key\" should not be null.", msg);
  }
  
}
