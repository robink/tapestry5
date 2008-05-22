// Copyright 2007, 2008 The Apache Software Foundation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.tapestry5.corelib.components;

import org.apache.tapestry5.*;
import org.apache.tapestry5.annotations.*;
import org.apache.tapestry5.beaneditor.BeanModel;
import org.apache.tapestry5.beaneditor.PropertyModel;
import org.apache.tapestry5.corelib.data.GridPagerPosition;
import org.apache.tapestry5.grid.*;
import org.apache.tapestry5.internal.TapestryInternalUtils;
import org.apache.tapestry5.internal.beaneditor.BeanModelUtils;
import org.apache.tapestry5.internal.bindings.AbstractBinding;
import org.apache.tapestry5.internal.services.ClientBehaviorSupport;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.internal.util.Defense;
import org.apache.tapestry5.services.BeanModelSource;
import org.apache.tapestry5.services.ComponentEventResultProcessor;
import org.apache.tapestry5.services.FormSupport;
import org.apache.tapestry5.services.Request;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * A grid presents tabular data. It is a composite component, created in terms of several sub-components. The
 * sub-components are statically wired to the Grid, as it provides access to the data and other models that they need.
 * <p/>
 * A Grid may operate inside a {@link org.apache.tapestry5.corelib.components.Form}. By overriding the cell renderers of
 * properties, the default output-only behavior can be changed to produce a complex form with individual control for
 * editing properties of each row. This is currently workable but less than ideal -- if the order of rows provided by
 * the {@link GridDataSource} changes between render and form submission, then there's the possibility that data will be
 * applied to the wrong server-side objects.
 *
 * @see org.apache.tapestry5.beaneditor.BeanModel
 * @see org.apache.tapestry5.services.BeanModelSource
 * @see org.apache.tapestry5.grid.GridDataSource
 */
@SupportsInformalParameters
public class Grid implements GridModel
{
    /**
     * The source of data for the Grid to display. This will usually be a List or array but can also be an explicit
     * {@link GridDataSource}. For Lists and object arrays, a GridDataSource is created automatically as a wrapper
     * around the underlying List.
     */
    @Parameter(required = true)
    private GridDataSource source;

    /**
     * A wrapper around the provided GridDataSource that caches access to the availableRows property. This is the source
     * provided to sub-components.
     */
    private GridDataSource cachingSource;

    /**
     * The number of rows of data displayed on each page. If there are more rows than will fit, the Grid will divide up
     * the rows into "pages" and (normally) provide a pager to allow the user to navigate within the overall result
     * set.
     */
    @Parameter("25")
    private int rowsPerPage;

    /**
     * Defines where the pager (used to navigate within the "pages" of results) should be displayed: "top", "bottom",
     * "both" or "none".
     */
    @Parameter(value = "top", defaultPrefix = BindingConstants.LITERAL)
    private GridPagerPosition pagerPosition;

    /**
     * Used to store the current object being rendered (for the current row). This is used when parameter blocks are
     * provided to override the default cell renderer for a particular column ... the components within the block can
     * use the property bound to the row parameter to know what they should render.
     */
    @Parameter
    private Object row;

    /**
     * The model used to identify the properties to be presented and the order of presentation. The model may be
     * omitted, in which case a default model is generated from the first object in the data source (this implies that
     * the objects provided by the source are uniform). The model may be explicitly specified to override the default
     * behavior, say to reorder or rename columns or add additional columns.
     */
    @Parameter
    private BeanModel model;

    /**
     * The model used to handle sorting of the Grid. This is generally not specified, and the built-in model supports
     * only single column sorting. The sort constraints (the column that is sorted, and ascending vs. descending) is
     * stored as persistent fields of the Grid component.
     */
    @Parameter
    private GridSortModel sortModel;

    /**
     * A comma-seperated list of property names to be added to the {@link org.apache.tapestry5.beaneditor.BeanModel}.
     * Cells for added columns will be blank unless a cell override is provided.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String add;

    /**
     * A comma-separated list of property names to be retained from the {@link org.apache.tapestry5.beaneditor.BeanModel}.
     * Only these properties will be retained, and the properties will also be reordered. The names are
     * case-insensitive.
     */
    @SuppressWarnings("unused")
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String include;

    /**
     * A comma-separated list of property names to be removed from the {@link org.apache.tapestry5.beaneditor.BeanModel}.
     * The names are case-insensitive.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String exclude;

    /**
     * A comma-separated list of property names indicating the order in which the properties should be presented. The
     * names are case insensitive. Any properties not indicated in the list will be appended to the end of the display
     * order.
     */
    @Parameter(defaultPrefix = BindingConstants.LITERAL)
    private String reorder;

    /**
     * A Block to render instead of the table (and pager, etc.) when the source is empty. The default is simply the text
     * "There is no data to display". This parameter is used to customize that message, possibly including components to
     * allow the user to create new objects.
     */
    @Parameter(value = "block:empty")
    private Block empty;


    /**
     * If true, then the CSS class on each &lt;TD&gt; and &lt;TH&gt; cell will be omitted, which can reduce the amount
     * of output from the component overall by a considerable amount. Leave this as false, the default, when you are
     * leveraging the CSS to customize the look and feel of particular columns.
     */
    @Parameter
    private boolean lean;

    /**
     * If true and the Loop is enclosed by a Form, then the normal state persisting logic is turned off. Defaults to
     * false, enabling state persisting within Forms. If a Grid is present for some reason within a Form, but does not
     * contain any form control components (such as {@link TextField}), then binding volatile to false will reduce the
     * amount of client-side state that must be persisted.
     */
    @Parameter(name = "volatile")
    private boolean volatileState;

    /**
     * The CSS class for the tr element for each data row. This can be used to highlight particular rows, or cycle
     * between CSS values (for the "zebra effect"). If null or not bound, then no particular CSS class value is used.
     */
    @Parameter(cache = false)
    @Property(write = false)
    private String rowClass;

    /**
     * CSS class for the &lt;table&gt; element.  In addition, informal parameters to the Grid are rendered in the table
     * element.
     */
    @Parameter(name = "class", defaultPrefix = BindingConstants.LITERAL, value = "t-data-grid")
    @Property(write = false)
    private String tableClass;

    /**
     * If true, then the Grid will be wrapped in an element that acts like a {@link
     * org.apache.tapestry5.corelib.components.Zone}; all the paging and sorting links will
     */
    @Parameter
    private boolean inPlace;

    /**
     * The name of the psuedo-zone that encloses the Grid.
     */
    @Property(write = false)
    private String zone;

    private boolean didRenderZoneDiv;

    @Persist
    private int currentPage = 1;

    @Persist
    private String sortColumnId;

    @Persist
    private boolean sortAscending = true;

    @Inject
    private ComponentResources resources;

    @Inject
    private BeanModelSource modelSource;

    @Environmental
    private ClientBehaviorSupport clientBehaviorSupport;

    @SuppressWarnings("unused")
    @Component(
            parameters = { "lean=inherit:lean", "overrides=componentResources", "zone=zone" })
    private GridColumns columns;

    @SuppressWarnings("unused")
    @Component(
            parameters = { "rowClass=rowClass", "rowsPerPage=rowsPerPage", "currentPage=currentPage", "row=row", "volatile=inherit:volatile", "lean=inherit:lean" })
    private GridRows rows;

    @Component(parameters = { "source=dataSource", "rowsPerPage=rowsPerPage", "currentPage=currentPage", "zone=zone" })
    private GridPager pager;

    @SuppressWarnings("unused")
    @Component(parameters = "to=pagerTop")
    private Delegate pagerTop;

    @SuppressWarnings("unused")
    @Component(parameters = "to=pagerBottom")
    private Delegate pagerBottom;

    @SuppressWarnings("unused")
    @Component(parameters = "class=tableClass", inheritInformalParameters = true)
    private Any table;

    @Environmental(false)
    private FormSupport formSupport;

    @Inject
    private Request request;

    @Environmental
    private RenderSupport renderSupport;

    /**
     * Set up via the traditional or Ajax component event request handler
     */
    @Environmental
    private ComponentEventResultProcessor componentEventResultProcessor;

    /**
     * A version of GridDataSource that caches the availableRows property. This addresses TAPESTRY-2245.
     */
    class CachingDataSource implements GridDataSource
    {
        private boolean availableRowsCached;

        private int availableRows;

        public int getAvailableRows()
        {
            if (!availableRowsCached)
            {
                availableRows = source.getAvailableRows();
                availableRowsCached = true;
            }

            return availableRows;
        }

        public void prepare(int startIndex, int endIndex, List<SortConstraint> sortConstraints)
        {
            source.prepare(startIndex, endIndex, sortConstraints);
        }

        public Object getRowValue(int index)
        {
            return source.getRowValue(index);
        }

        public Class getRowType()
        {
            return source.getRowType();
        }
    }

    /**
     * Default implementation that only allows a single column to be the sort column, and stores the sort information as
     * persistent fields of the Grid component.
     */
    class DefaultGridSortModel implements GridSortModel
    {
        public ColumnSort getColumnSort(String columnId)
        {
            if (!TapestryInternalUtils.isEqual(columnId, sortColumnId))
                return ColumnSort.UNSORTED;

            return getColumnSort();
        }

        private ColumnSort getColumnSort()
        {
            return sortAscending ? ColumnSort.ASCENDING : ColumnSort.DESCENDING;
        }


        public void updateSort(String columnId)
        {
            Defense.notBlank(columnId, "columnId");

            if (columnId.equals(sortColumnId))
            {
                sortAscending = !sortAscending;
                return;
            }

            sortColumnId = columnId;
            sortAscending = true;
        }

        public List<SortConstraint> getSortContraints()
        {
            if (sortColumnId == null)
                return Collections.emptyList();

            PropertyModel sortModel = model.getById(sortColumnId);

            SortConstraint constraint = new SortConstraint(sortModel, getColumnSort());

            return Collections.singletonList(constraint);
        }

        public void clear()
        {
            sortColumnId = null;
        }
    }

    GridSortModel defaultSortModel()
    {
        return new DefaultGridSortModel();
    }

    Binding defaultModel()
    {
        final ComponentResources containerResources = resources.getContainerResources();

        return new AbstractBinding()
        {

            public Object get()
            {
                // Get the default row type from the data source

                Class rowType = source.getRowType();

                if (rowType == null) throw new RuntimeException(
                        "Unable to determine the bean type for rows from the GridDataSource. You should bind the model parameter explicitly.");

                // Properties do not have to be read/write

                return modelSource.create(rowType, false, containerResources);
            }

            /**
             * Returns false. This may be overkill, but it basically exists because the model is
             * inherently mutable and therefore may contain client-specific state and needs to be
             * discarded at the end of the request. If the model were immutable, then we could leave
             * invariant as true.
             */
            @Override
            public boolean isInvariant()
            {
                return false;
            }
        };
    }

    static final ComponentAction<Grid> SETUP_DATA_SOURCE = new ComponentAction<Grid>()
    {
        private static final long serialVersionUID = 8545187927995722789L;

        public void execute(Grid component)
        {
            component.setupDataSource();
        }
    };

    Object setupRender()
    {
        if (!volatileState && formSupport != null) formSupport.store(this, SETUP_DATA_SOURCE);

        setupDataSource();

        return cachingSource.getAvailableRows() == 0 ? empty : null;
    }

    void setupDataSource()
    {
        // If there's no rows, display the empty block placeholder.

        cachingSource = new CachingDataSource();

        int availableRows = cachingSource.getAvailableRows();

        if (availableRows == 0) return;

        BeanModelUtils.modify(model, add, include, exclude, reorder);

        int maxPage = ((availableRows - 1) / rowsPerPage) + 1;

        // This captures when the number of rows has decreased, typically due to deletions.

        if (currentPage > maxPage)
            currentPage = maxPage;

        int startIndex = (currentPage - 1) * rowsPerPage;

        int endIndex = Math.min(startIndex + rowsPerPage - 1, availableRows - 1);

        cachingSource.prepare(startIndex, endIndex, sortModel.getSortContraints());

    }

    Object beginRender(MarkupWriter writer)
    {
        // Skip rendering of component (template, body, etc.) when there's nothing to display.
        // The empty placeholder will already have rendered.

        if (cachingSource.getAvailableRows() == 0) return false;

        if (inPlace && zone == null)
        {
            zone = renderSupport.allocateClientId(resources);

            writer.element("div", "id", zone);

            clientBehaviorSupport.addZone(zone, null, "show");

            didRenderZoneDiv = true;
        }

        return null;
    }

    void afterRender(MarkupWriter writer)
    {
        if (didRenderZoneDiv)
        {
            writer.end(); // div
            didRenderZoneDiv = false;
        }
    }

    public BeanModel getDataModel()
    {
        return model;
    }

    public GridDataSource getDataSource()
    {
        return cachingSource;
    }

    public GridSortModel getSortModel()
    {
        return sortModel;
    }

    public Object getPagerTop()
    {
        return pagerPosition.isMatchTop() ? pager : null;
    }

    public Object getPagerBottom()
    {
        return pagerPosition.isMatchBottom() ? pager : null;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public void setCurrentPage(int currentPage)
    {
        this.currentPage = currentPage;
    }

    public int getRowsPerPage()
    {
        return rowsPerPage;
    }

    public Object getRow()
    {
        return row;
    }

    public void setRow(Object row)
    {
        this.row = row;
    }

    /**
     * Resets the Grid to inital settings; this sets the current page to one, and {@linkplain
     * org.apache.tapestry5.grid.GridSortModel#clear() clears the sort model}.
     */
    public void reset()
    {
        currentPage = 1;
        sortModel.clear();
    }

    /**
     * Event handler for inplaceupdate event triggered from nested components when an Ajax update occurs. The event
     * context will carry the zone, which is recorded here, to allow the Grid and its sub-components to properly
     * re-render themselves.  Invokes {@link org.apache.tapestry5.services.ComponentEventResultProcessor#processResultValue(Object)}
     * passing this (the Grid component) as the content provider for the update.
     */
    void onInPlaceUpdate(String zone) throws IOException
    {
        this.zone = zone;

        componentEventResultProcessor.processResultValue(this);
    }
}