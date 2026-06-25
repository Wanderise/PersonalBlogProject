package com.third.common.result;
import io.swagger.v3.oas.annotations.media.Schema;

public class Result<E> {
    @Schema(description = "状态码")
    private int code;
    @Schema(description = "错误信息")
    private String msg;
    @Schema(description = "数据")
    private E data;

    public Result() {
    }

    public Result(int code, String msg, E data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <E> Result<E> success(){
        Result<E> r = new Result<E>();
        r.code = 200;
        return r;
    }

    public static <E> Result<E> success(E data){
        Result<E> r = new Result<E>();
        r.code = 200;
        r.data = data;
        return r;
    }

    public static <E> Result error(String msg){
        return error(400, msg);
    }

    public static <E> Result<E> error(int code, String msg){
        Result<E> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }



    public String toString(){
        return "code=" + code + ", msg=" + msg + ", data=" + data;
    }

    /**
     * 获取
     * @return code
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置
     * @param code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取
     * @return msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置
     * @param msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取
     * @return data
     */
    public E getData() {
        return data;
    }

    /**
     * 设置
     * @param data
     */
    public void setData(E data) {
        this.data = data;
    }
}
