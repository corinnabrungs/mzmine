/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.parameters.parametertypes.elements;

import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.openscience.cdk.Element;

public class ElementsParameter implements UserParameter<List<Element>, ElementsComponent> {

  private final String name, description;
  private final boolean valueRequired;
  private List<Element> value;

  public ElementsParameter(String name, String description, boolean valueRequired,
      List<Element> defaultValue) {

    this.name = name;
    this.description = description;
    this.valueRequired = valueRequired;
    this.value = defaultValue;
  }

  public ElementsParameter(String name, String description) {
    // Most abundance elements in biomolecules as a default value for elements
    this(name, description, true, Arrays.asList(new Element("H"),
        new Element("C"), new Element("N"), new Element("O"), new Element("P"),
        new Element("S")));
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ElementsComponent createEditingComponent() {
    return new ElementsComponent(this.value);
  }

  @Override
  public void setValueFromComponent(ElementsComponent elementsComponent) {
    this.value = elementsComponent.getValue();
  }

  @Override
  public void setValueToComponent(ElementsComponent elementsComponent, List<Element> newValue) {
    elementsComponent.setValue(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public List<Element> getValue() {
    return value;
  }

  @Override
  public void setValue(List<Element> value) {
    this.value = value;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value.isEmpty()) {
      errorMessages.add("There are no chemical elements selected.");
      return false;
    }

    return true;
  }

  @Override
  public void loadValueFromXML(org.w3c.dom.Element xmlElement) {
    if (xmlElement == null) {
      throw new NullPointerException("XML Element is null.");
    }

    String values = xmlElement.getTextContent().replaceAll("\\s", "");
    value = Arrays.stream(values.split(","))
        .map(Element::new)
        .collect(Collectors.toList());
  }

  @Override
  public void saveValueToXML(org.w3c.dom.Element xmlElement) {
    if (xmlElement == null) {
      throw new NullPointerException("XML Element is null.");
    }

    if (value == null) {
      throw new NullPointerException("Value is null.");
    }

    String text = value.stream()
        .map(Element::getSymbol)
        .map(Object::toString)
        .collect(Collectors.joining(","));

    xmlElement.setTextContent(text);
  }

  @Override
  public boolean isSensitive() {
    return false;
  }

  @Override
  public UserParameter<List<Element>, ElementsComponent> cloneParameter() {
    ElementsParameter copy = new ElementsParameter(name, description, valueRequired, value);
    copy.setValue(this.getValue());
    return copy;
  }

}
