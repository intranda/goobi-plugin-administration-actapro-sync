package io.goobi.api.job.actapro.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageableObject {

    @JsonProperty("offset")
    private Long offset = null;

    //    @JsonProperty("sort")
    //    private List<SortObject> sort = null;

    @JsonProperty("paged")
    private Boolean paged = null;

    @JsonProperty("unpaged")
    private Boolean unpaged = null;

    @JsonProperty("pageSize")
    private Integer pageSize = null;

    @JsonProperty("pageNumber")
    private Integer pageNumber = null;

    public PageableObject offset(Long offset) {
        this.offset = offset;
        return this;
    }

    public PageableObject sort(List<SortObject> sort) {
        //        this.sort = sort;
        return this;
    }

    public PageableObject addSortItem(SortObject sortItem) {
        //        if (sort == null) {
        //            sort = new ArrayList<>();
        //        }
        //        this.sort.add(sortItem);
        return this;
    }

    public PageableObject paged(Boolean paged) {
        this.paged = paged;
        return this;
    }

    public PageableObject unpaged(Boolean unpaged) {
        this.unpaged = unpaged;
        return this;
    }

    public PageableObject pageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageableObject pageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PageableObject {\n");

        sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
        //        sb.append("    sort: ").append(toIndentedString(sort)).append("\n");
        sb.append("    paged: ").append(toIndentedString(paged)).append("\n");
        sb.append("    unpaged: ").append(toIndentedString(unpaged)).append("\n");
        sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
        sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces (except the first line).
     */
    private static String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
