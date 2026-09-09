import {mkdir,copyFile} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
process.chdir(fileURLToPath(new URL('.',import.meta.url)));
await mkdir('dist',{recursive:true});
for (const name of ['index.html','app.js','engine.js','style.css','sw.js','manifest.webmanifest','icon.svg','icon-180.png','icon-192.png','icon-512.png']) await copyFile(name,`dist/${name}`);
console.log('Application statique prête : dist');
