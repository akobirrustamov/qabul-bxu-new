import axios from "axios";
export let baseUrl;
baseUrl = "http://localhost:8080";
// baseUrl = "https://qabul.bxu.uz";
// baseUrl = "";
export default function (url, method, data, param, is_user=false) {
  let token = localStorage.getItem("access_token");
  // console.log(is_user)
  if(is_user){
    token =  localStorage.getItem("browser_token");
    // console.log(token)
  }
  // console.log(param)
  return axios({
    url: baseUrl + url,
    method: method,
    data: data,
    headers: {
      Authorization: token,
    },
    params: param,
  })
    .then((res) => {
      if (res.data) {
        // console.log(res.data)
        return {
          error: false,
          data: res.data,
        };
      }
    })
    .catch((err) => {
      if (err.response.status === 401) {
        if (localStorage.getItem("refresh_token") === null) {
          return {
            error: true,
            data: err.response.status,
          };
        }
      }
    });
}