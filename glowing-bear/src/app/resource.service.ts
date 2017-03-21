import { Injectable } from '@angular/core';
import { Http, Response, Headers } from '@angular/http';
import { Study } from './study';

import 'rxjs/add/operator/toPromise';

@Injectable()
export class ResourceService {

  constructor(private http: Http) { }

  getStudies(): Promise<Study[]> {
    var headers = new Headers();
    headers.append('Authorization', 'Bearer ');
    return this.http.get('http://localhost:8080/v2/studies', {
      headers: headers
    })
      .toPromise()
      .then((response: Response) => {
        return response.json().studies as Study[]
      })
      .catch(() => {
        console.log("an error occurred");
      });
  }

}
