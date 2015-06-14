/*
 * Copyright (c) 2015 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.datatorrent.lib.dimensions;

import com.datatorrent.api.Context.OperatorContext;
import com.datatorrent.lib.appdata.schemas.DimensionalConfigurationSchema;
import com.datatorrent.lib.appdata.schemas.FieldsDescriptor;
import com.datatorrent.lib.dimensions.DimensionsEvent.InputEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import javax.validation.constraints.NotNull;

import java.util.List;

/**
 * This is an implementation of a generic dimensions computation operator, which processes input data according to
 * a single {@link DimensionalConfigurationSchema}. The JSON defining a {@link DimensionalConfigurationSchema} can be
 * set on the operator as a property.
 * @param <INPUT> The input data type.
 */
public abstract class AbstractDimensionsComputationFlexibleSingleSchema<INPUT> extends
                      AbstractDimensionsComputationFlexible<INPUT>
{
  /**
   * The default schema ID.
   */
  public static final int DEFAULT_SCHEMA_ID = 1;

  /**
   * This holds the JSON which defines the {@link DimensionalConfigurationSchema} to be used by this operator.
   */
  @NotNull
  private String configurationSchemaJSON;
  /**
   * The {@link DimensionalConfigurationSchema} to be used by this operator.
   */
  protected DimensionalConfigurationSchema configurationSchema;

  /**
   * Conversion context holder.
   */
  private transient final DimensionsConversionContext conversionContext = new DimensionsConversionContext();
  /**
   * The schemaID applied to {@link Aggregate}s generated by this operator.
   */
  private int schemaID = DEFAULT_SCHEMA_ID;

  public AbstractDimensionsComputationFlexibleSingleSchema()
  {
  }

  @Override
  public void setup(OperatorContext context)
  {
    super.setup(context);

    //If the configuration schema is not already set, create it from the given JSON.
    if(configurationSchema == null) {
      configurationSchema = new DimensionalConfigurationSchema(configurationSchemaJSON,
                                                               aggregatorRegistry);
    }
  }

  @Override
  public void processInputEvent(INPUT input)
  {
    List<FieldsDescriptor> keyFieldsDescriptors = configurationSchema.getDimensionsDescriptorIDToKeyDescriptor();

    for(int ddID = 0;
        ddID < keyFieldsDescriptors.size();
        ddID++) {
      //Create the conversion context for the conversion.
      FieldsDescriptor keyFieldsDescriptor = keyFieldsDescriptors.get(ddID);
      Int2ObjectMap<FieldsDescriptor> map = configurationSchema.getDimensionsDescriptorIDToAggregatorIDToInputAggregatorDescriptor().get(ddID);
      IntArrayList aggIDList = configurationSchema.getDimensionsDescriptorIDToAggregatorIDs().get(ddID);
      DimensionsDescriptor dd = configurationSchema.getDimensionsDescriptorIDToDimensionsDescriptor().get(ddID);

      for(int aggIDIndex = 0;
          aggIDIndex < aggIDList.size();
          aggIDIndex++) {
        int aggID = aggIDList.get(aggIDIndex);

        conversionContext.schemaID = schemaID;
        conversionContext.dimensionDescriptorID = ddID;
        conversionContext.aggregatorID = aggID;
        conversionContext.dimensionDescriptorID = ddID;

        conversionContext.dd = dd;
        conversionContext.keyFieldsDescriptor = keyFieldsDescriptor;
        conversionContext.aggregateDescriptor = map.get(aggID);

        InputEvent inputE = convertInput(input,
                                         conversionContext);

        int aggregateIndex = this.aggregatorIdToAggregateIndex.get(conversionContext.aggregatorID);
        processConvertedInput(aggregateIndex,
                              inputE);
      }
    }
  }

  /**
   * This is a helper method to process an input event which has been converted to an {@link InputEvent}.
   * This helper method is useful for cases in which the way {@link InputEvent}s are handled by the operator.
   * By default this method will aggregate the {@link InputEvent} in an {@link AggregateMap}, which has
   * its contents dumped out at the end of the window. However, if a pass through operator is required, this method
   * could be override to immediately emit an {@link Aggregate}.
   * @param aggregateIndex The aggregateIndex corresponding to the input event.
   * @param inputE The converted input event.
   */
  public void processConvertedInput(int aggregateIndex,
                                    InputEvent inputE)
  {
    this.maps[aggregateIndex].aggregate(inputE);
  }

  /**
   * Returns the schemaID for {@link Aggregate}s generated by this operator.
   * @return The schemaID for {@link Aggregate}s generated by this operator.
   */
  public int getSchemaID()
  {
    return schemaID;
  }

  /**
   * Sets the schemaID for {@link Aggregate}s generated by this operator.
   * @param schemaID The schemaID for {@link Aggregate}s generated by this operator.
   */
  public void setSchemaID(int schemaID)
  {
    this.schemaID = schemaID;
  }

  /**
   * Returns the configurationSchemaJSON used by this operator.
   * @return The configurationSchemaJSON used by this operator.
   */
  public String getConfigurationSchemaJSON()
  {
    return configurationSchemaJSON;
  }

  /**
   * Sets the configurationSchemaJSON used by this operator.
   * @param configurationSchemaJSON The configurationSchemaJSON used by this operator.
   */
  public void setConfigurationSchemaJSON(String configurationSchemaJSON)
  {
    this.configurationSchemaJSON = configurationSchemaJSON;
  }
}
