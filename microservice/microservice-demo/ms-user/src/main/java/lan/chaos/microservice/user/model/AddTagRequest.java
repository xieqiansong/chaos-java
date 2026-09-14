package lan.chaos.microservice.user.model;

import javax.validation.constraints.NotBlank;

/**
 * 给用户添加标签的请求体。
 */
public class AddTagRequest {

    @NotBlank(message = "标签不能为空")
    private String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
