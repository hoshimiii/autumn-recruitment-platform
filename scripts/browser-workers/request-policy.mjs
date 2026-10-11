import { requireOrigin, AccessBarrierError } from './protocol.mjs';

export async function installRequestPolicy(context,{origin,leaseExpiresAt,check,pace,stop,fail=async()=>{},onDocumentUrl=()=>{}}) {
  let failure,closing,finishing=false,active=0;const drained=[];
  const halt=error=>{
    if(failure)return closing;
    failure=error instanceof Error?error:new Error(error);
    const notify=failure instanceof AccessBarrierError?stop:fail;
    closing=Promise.all([context.close().catch(()=>{}),Promise.resolve().then(()=>notify(failure.message)).catch(()=>{})]);
    return closing;
  };
  await context.route('**/*',async route=>{
    if(failure || finishing){await route.abort().catch(()=>{});return;}
    active++;
    const request=route.request();let response;
    try {
      if(!['GET','HEAD'].includes(request.method()))throw new AccessBarrierError('Source mutation request rejected');
      const url=request.url();
        if(failure)throw failure;
        requireOrigin(url,origin);
        await check(url);
        const readAt=await pace(url);
        if(readAt>leaseExpiresAt-20_000)throw new Error('Source pacing exceeds task lease budget');
        await new Promise(resolve=>setTimeout(resolve,Math.max(0,readAt-Date.now())));
        if(failure)throw failure;
        await check(url);
        if(failure)throw failure;
        // Playwright routes only the first redirect URL; never dispatch an ungated target.
        response=await route.fetch({url,maxRedirects:0,timeout:15_000});
        if([401,403,429].includes(response.status()))throw new AccessBarrierError(`HTTP access barrier ${response.status()}`);
        if([301,302,303,307,308].includes(response.status())) {
          const location=response.headers().location;
          if(location) {const next=new URL(location,url).href;requireOrigin(next,origin);await check(next);}
          throw new Error('Source redirect rejected; configure the approved final URL');
        }
        if(failure)throw failure;
        if(request.isNavigationRequest() && request.frame()===request.frame().page().mainFrame())onDocumentUrl(url);
        await route.fulfill({response});await response.dispose();response=undefined;return;
    } catch(error) {
      const stopping=halt(error);
      await response?.dispose().catch(()=>{});
      await route.abort().catch(()=>{});
      await stopping;
    } finally {
      active--;if(!active)drained.splice(0).forEach(resolve=>resolve());
    }
  });
  await context.routeWebSocket('**/*',async socket=>{const stopping=halt(new AccessBarrierError('Source WebSocket channel rejected'));await socket.close().catch(()=>{});await stopping;});
  context.on('page',page=>{
    page.on('download',download=>{void download.cancel();void halt(new AccessBarrierError('Unexpected source download'));});
    page.on('response',response=>{if([401,403,429].includes(response.status()))void halt(new AccessBarrierError(`HTTP access barrier ${response.status()}`));});
  });
  return {get error(){return failure;},stop:halt,finish:async()=>{
    while(active && !failure)await new Promise(resolve=>drained.push(resolve));
    finishing=true;await context.close().catch(()=>{});
  }};
}
