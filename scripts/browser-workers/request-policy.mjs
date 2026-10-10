import { requireOrigin } from './protocol.mjs';

export async function installRequestPolicy(context,{origin,leaseExpiresAt,check,pace,stop,onDocumentUrl=()=>{}}) {
  let barrier,closing,finishing=false,active=0;const drained=[];
  const halt=reason=>{
    if(barrier)return closing;
    barrier=reason;
    closing=Promise.all([context.close().catch(()=>{}),Promise.resolve().then(()=>stop(reason)).catch(()=>{})]);
    return closing;
  };
  await context.route('**/*',async route=>{
    if(barrier || finishing){await route.abort().catch(()=>{});return;}
    active++;
    const request=route.request();let response;
    try {
      if(!['GET','HEAD'].includes(request.method()))throw new Error('Source mutation request rejected');
      const url=request.url();
        if(barrier)throw new Error(barrier);
        requireOrigin(url,origin);
        await check(url);
        const readAt=await pace(url);
        if(readAt>leaseExpiresAt-20_000)throw new Error('Source pacing exceeds task lease budget');
        await new Promise(resolve=>setTimeout(resolve,Math.max(0,readAt-Date.now())));
        if(barrier)throw new Error(barrier);
        await check(url);
        if(barrier)throw new Error(barrier);
        // Playwright routes only the first redirect URL; never dispatch an ungated target.
        response=await route.fetch({url,maxRedirects:0,timeout:15_000});
        if([401,403,429].includes(response.status()))throw new Error(`HTTP access barrier ${response.status()}`);
        if([301,302,303,307,308].includes(response.status())) {
          const location=response.headers().location;
          if(location) {const next=new URL(location,url).href;requireOrigin(next,origin);await check(next);}
          throw new Error('Source redirect rejected; configure the approved final URL');
        }
        if(barrier)throw new Error(barrier);
        if(request.isNavigationRequest() && request.frame()===request.frame().page().mainFrame())onDocumentUrl(url);
        await route.fulfill({response});await response.dispose();response=undefined;return;
    } catch(error) {
      const stopping=halt(error.message);
      await response?.dispose().catch(()=>{});
      await route.abort().catch(()=>{});
      await stopping;
    } finally {
      active--;if(!active)drained.splice(0).forEach(resolve=>resolve());
    }
  });
  await context.routeWebSocket('**/*',async socket=>{const stopping=halt('Source WebSocket channel rejected');await socket.close().catch(()=>{});await stopping;});
  context.on('page',page=>{
    page.on('download',download=>{void download.cancel();void halt('Unexpected source download');});
    page.on('response',response=>{if([401,403,429].includes(response.status()))void halt(`HTTP access barrier ${response.status()}`);});
  });
  return {get barrier(){return barrier;},stop:halt,finish:async()=>{
    while(active && !barrier)await new Promise(resolve=>drained.push(resolve));
    finishing=true;await context.close().catch(()=>{});
  }};
}
