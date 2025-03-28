package io.goobi.api.job.actapro.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResultPage {

    @JsonProperty("content")
    private List<Map<String, String>> content = null;

    @JsonProperty("pageable")
    private PageableObject pageable = null;

    @JsonProperty("totalPages")
    private Integer totalPages = null;

    @JsonProperty("totalElements")
    private Long totalElements = null;

    @JsonProperty("last")
    private Boolean last = null;

    @JsonProperty("size")
    private Integer size = null;

    @JsonProperty("number")
    private Integer number = null;

    //    @JsonProperty("sort")
    //    private List<SortObject> sort = null;

    @JsonProperty("first")
    private Boolean first = null;

    @JsonProperty("numberOfElements")
    private Integer numberOfElements = null;

    @JsonProperty("empty")
    private Boolean empty = null;

    public SearchResultPage content(List<Map<String, String>> content) {
        this.content = content;
        return this;
    }

    public SearchResultPage addContentItem(Map<String, String> contentItem) {
        if (content == null) {
            content = new ArrayList<>();
        }
        this.content.add(contentItem);
        return this;
    }

    public SearchResultPage pageable(PageableObject pageable) {
        this.pageable = pageable;
        return this;
    }

    public SearchResultPage totalPages(Integer totalPages) {
        this.totalPages = totalPages;
        return this;
    }

    public SearchResultPage totalElements(Long totalElements) {
        this.totalElements = totalElements;
        return this;
    }

    public SearchResultPage last(Boolean last) {
        this.last = last;
        return this;
    }

    public SearchResultPage size(Integer size) {
        this.size = size;
        return this;
    }

    public SearchResultPage number(Integer number) {
        this.number = number;
        return this;
    }

    public SearchResultPage sort(List<SortObject> sort) {
        //        this.sort = sort;
        return this;
    }

    public SearchResultPage addSortItem(SortObject sortItem) {
        //        this.sort.add(sortItem);
        return this;
    }

    public SearchResultPage first(Boolean first) {
        this.first = first;
        return this;
    }

    public SearchResultPage numberOfElements(Integer numberOfElements) {
        this.numberOfElements = numberOfElements;
        return this;
    }

    public SearchResultPage empty(Boolean empty) {
        this.empty = empty;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SearchResultPage {\n");

        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        //        sb.append("    pageable: ").append(toIndentedString(pageable)).append("\n");
        sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
        sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
        sb.append("    last: ").append(toIndentedString(last)).append("\n");
        sb.append("    size: ").append(toIndentedString(size)).append("\n");
        sb.append("    number: ").append(toIndentedString(number)).append("\n");
        //        sb.append("    sort: ").append(toIndentedString(sort)).append("\n");
        sb.append("    first: ").append(toIndentedString(first)).append("\n");
        sb.append("    numberOfElements: ").append(toIndentedString(numberOfElements)).append("\n");
        sb.append("    empty: ").append(toIndentedString(empty)).append("\n");
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
