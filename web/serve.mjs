import http from 'node:http';
import {readFile} from 'node:fs/promises';
const mime={html:'text/html',js:'text/javascript',css:'text/css',svg:'image/svg+xml',webmanifest:'application/manifest+json'};
http.createServer(async(req,res)=>{try{const name=new URL(req.url,'http://localhost').pathname.slice(1)||'index.html';if(!/^[a-zA-Z0-9.-]+$/.test(name))throw Error();const data=await readFile(new URL(name,import.meta.url));res.writeHead(200,{'Content-Type':mime[name.split('.').pop()]||'application/octet-stream','Cache-Control':'no-cache'});res.end(data);}catch{res.writeHead(404);res.end();}}).listen(5173,'0.0.0.0',()=>console.log('Local: http://localhost:5173'));
