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

package com.google.k2crypto.storage;

/**
 * Annotation applied to all {@link StoreDriver} implementations.
 * 
 * @author darylseah@gmail.com (Daryl Seah)
 */
public @interface StoreDriverInfo {
  
  /**
   * Unique identifier of the driver, which users will specify in the scheme
   * portion of the URI address to a store. 
   * 
   * Legal identifiers must match the regular expression:
   * {@code [A-Za-z][A-Za-z0-9\+\-\.]*}
   * 
   * @see
   * <a href="http://tools.ietf.org/html/rfc3986#section-3.1" target="_blank">
   * RFC 3986 (URI: Generic Syntax), Section 3.1</a>
   */
  String id();
  
  /**
   * Descriptive name of the driver.
   */
  String name();
  
  /**
   * Version string of the driver.
   */
  String version();
  
  /**
   * Whether the driver can only read keys and not write them.
   * Defaults to false.
   */
  boolean readOnly() default false;
  
  /**
   * Whether the driver supports wrapped (encrypted) keys.
   * Defaults to false. 
   */
  boolean wrapSupport() default false;

}

