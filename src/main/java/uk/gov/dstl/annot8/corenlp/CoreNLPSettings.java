/*
 * Crown Copyright (C) 2021 Dstl
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package uk.gov.dstl.annot8.corenlp;

import io.annot8.api.settings.Description;

import java.util.Properties;

public class CoreNLPSettings implements io.annot8.api.settings.Settings {

  protected Properties properties;

  public CoreNLPSettings(){
    properties = new Properties();
  }

  public CoreNLPSettings(Properties properties){
    this.properties = properties;
  }

  @Description("Properties to pass to the CoreNLP annotator (including the prefix)")
  public Properties getProperties() {
    return properties;
  }
  public void setProperties(Properties properties) {
    this.properties = properties;
  }
  public void addProperty(String key, String value){
    properties.put(key, value);
  }

  @Override
  public boolean validate() {
    return properties != null;
  }
}