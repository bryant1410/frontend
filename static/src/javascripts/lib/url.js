// @flow

import detect from 'lib/detect';

const supportsPushState = detect.hasPushStateSupport();

// returns "foo=bar&fizz=buzz" (eg. no ? symbol)
function getCurrentQueryString(): string {
    return window.location.search.replace(/^\?/, '');
}

// returns a map of querystrings
// eg ?foo=bar&fizz=buzz returns {foo: 'bar', fizz: 'buzz'}
function getUrlVars(query?: string): Object {
    return (query || getCurrentQueryString())
        .split('&')
        .filter(Boolean)
        .map(param => param.includes('=') ? param.split('=') : [param, true])
        .reduce(
            (acc, input) => {
                const result = acc;
                result[input[0]] = input[1];
                return result;
            },
            {}
        );
}

function updateQueryString(params: Object, historyFn: Function) {
    const querystringChanged = getCurrentQueryString() !== params.querystring;

    if (params.querystring && querystringChanged && supportsPushState) {
        historyFn(
            params.state || {},
            params.title || window.title,
            params.querystring + window.location.hash
        );
    }
}

// this will replace anything after the root/domain of the URL
// and add an item to the browser history.
// params Object requires a "querystring" property
// and optionally takes a "state" and "title" property too
function pushQueryString(params: Object) {
    return updateQueryString(params, history.pushState.bind(history));
}

// equivalent to pushQueryString but uses history.replaceState to
// overwrite history rather than history.pushState
function replaceQueryString(params: Object) {
    return updateQueryString(params, history.replaceState.bind(history));
}

// take an Object, construct into a query, e.g. {page: 1, pageSize: 10} => page=1&pageSize=10
function constructQuery(query: Object): string {
    return Object.keys(query)
        .map(param => {
            const value = query[param];
            const queryValue = Array.isArray(value) ? value.join(',') : value;
            return `${param}=${queryValue}`;
        })
        .join('&');
}

function getPath(url: string): string {
    const a: HTMLAnchorElement = document.createElement('a');
    a.href = url;
    return a.pathname;
}

function pushUrl(
    state: Object,
    title: string,
    url: string,
    replace?: boolean = false
): void {
    if (supportsPushState) {
        window.history[replace ? 'replaceState' : 'pushState'](
            state,
            title,
            url
        );
    }
}

function back(): void {
    if (supportsPushState) {
        window.history.back();
    }
}

export default {
    getUrlVars,
    getPath,
    pushUrl,
    constructQuery,
    back,
    supportsPushState,
    pushQueryString,
    replaceQueryString,
};