/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// domui.js: SVG UI components
//

Browser.prototype.removeAllPopups = function() {
    removeChildren(this.hPopupHolder);
    removeChildren(this.popupHolder);
}

Browser.prototype.makeTooltip = function(ele, text)
{
    var isin = false;
    var thisB = this;
    var timer = null;
    var outlistener;
    outlistener = function(ev) {
        isin = false;
        if (timer) {
            clearTimeout(timer);
            timer = null;
        }
        ele.removeEventListener('mouseout', outlistener, false);
    };

    var setup;
    setup = function(ev) {

        // [rnugraha] fix tooltip elements
        // -- start customization --

        // var mx = ev.clientX + window.scrollX, my = ev.clientY + window.scrollY;

        var mouseOver = myHandleEvent(ev);
        var top = mouseOver.y;
        var left = mouseOver.x;

        if (!timer) {
            timer = setTimeout(function() {
                var popup = makeElement('div',
                    [makeElement('div', null, {className: 'tooltip-arrow'}),
                     makeElement('div', text, {className: 'tooltip-inner'})], 
                    // {className: 'tooltip bottom in'}, {
                    {className: 'tooltip in'}, {
                    display: 'block',
                    // top: '' + (my + 20) + 'px',
                    // left: '' + Math.max(mx - 30, 20) + 'px'
                    top: '' + (top + 10) + 'px',
                    left: '' + left + 'px'
                });
                thisB.hPopupHolder.appendChild(popup);
                var moveHandler;
                moveHandler = function(ev) {
                    try {
                        thisB.hPopupHolder.removeChild(popup);
                    } catch (e) {
                        // May have been removed by other code which clears the popup layer.
                    }
                    window.removeEventListener('mousemove', moveHandler, false);
                    if (isin) {
                        if (ele.offsetParent == null) {
                            // dlog('Null parent...');
                        } else {
                            setup(ev);
                        }
                    }
                }
                window.addEventListener('mousemove', moveHandler, false);
                timer = null;
            }, 1000);
        }
    };

    ele.addEventListener('mouseover', function(ev) {
        isin = true
        ele.addEventListener('mouseout', outlistener, false);
        setup(ev);
    }, false);
    ele.addEventListener('DOMNodeRemovedFromDocument', function(ev) {
        isin = false;
        if (timer) {
            clearTimeout(timer);
            timer = null;
        }
    }, false);
}

/**
 * [rnugraha] helper function to return the mouse position (x & y)
 * @param e
 * @returns {{x: number, y: number}}
 */
function myHandleEvent(e) {
    var evt = e ? e:window.event;
    var clickX=0, clickY=0;

    if ((evt.clientX || evt.clientY) &&
        document.body &&
        document.body.scrollLeft!=null) {
        clickX = evt.clientX + document.body.scrollLeft;
        clickY = evt.clientY + document.body.scrollTop;
    }
    if ((evt.clientX || evt.clientY) &&
        document.compatMode=='CSS1Compat' &&
        document.documentElement &&
        document.documentElement.scrollLeft!=null) {
        clickX = evt.clientX + document.documentElement.scrollLeft;
        clickY = evt.clientY + document.documentElement.scrollTop;
    }
    if (evt.pageX || evt.pageY) {
        clickX = evt.pageX;
        clickY = evt.pageY;
    }
    return {x:clickX, y:clickY};
}

/**
 * [rnugraha] helper function to get position of element
 * @param element
 * @returns {{x: number, y: number}}
 */
function getPosition(element) {
    var xPosition = 0;
    var yPosition = 0;

    while(element) {
        xPosition += (element.offsetLeft - element.scrollLeft + element.clientLeft);
        yPosition += (element.offsetTop - element.scrollTop + element.clientTop);
        element = element.offsetParent;
    }
    return { x: xPosition, y: yPosition };
}


Browser.prototype.popit = function(ev, name, ele, opts)
{
    var thisB = this;
    if (!opts) 
        opts = {};
    if (!ev) 
        ev = {};

    var width = opts.width || 200;


    // [rnugraha] fix popup elements
    //  -- start customization --

//    var mx, my;
//
//    if (ev.clientX) {
//        var mx =  ev.clientX, my = ev.clientY;
//    } else {
//        mx = 500; my= 50;
//    }
//    mx +=  document.documentElement.scrollLeft || document.body.scrollLeft;
//    my +=  document.documentElement.scrollTop || document.body.scrollTop;
//
//    var winWidth = window.innerWidth;
//
//    var top = (my + 20);
//    var left = Math.min(mx - (width/2), (winWidth - width - 30));

    var winWidth = window.innerWidth;
    var mouseClick = myHandleEvent(ev);

    var top = mouseClick.y;
    var left = mouseClick.x - (width/2);

    // -- end customization --


    var popup = makeElement('div');
    popup.className = 'popover fade ' + (ev.clientX ? 'bottom ' : '') + 'in';
    popup.style.display = 'block';
    popup.style.position = 'absolute';
    popup.style.top = '' + top + 'px';
    popup.style.left = '' + left + 'px';
    popup.style.width = width + 'px';
    if (width > 276) {
        // HACK Bootstrappification...
        popup.style.maxWidth = width + 'px';
    }

    popup.appendChild(makeElement('div', null, {className: 'arrow'}));

    if (name) {
        var closeButton = makeElement('button', '', {className: 'close'});
        closeButton.innerHTML = '&times;'

        closeButton.addEventListener('mouseover', function(ev) {
            closeButton.style.color = 'red';
        }, false);
        closeButton.addEventListener('mouseout', function(ev) {
            closeButton.style.color = 'black';
        }, false);
        closeButton.addEventListener('click', function(ev) {
            ev.preventDefault(); ev.stopPropagation();
            thisB.removeAllPopups();
        }, false);
        var tbar = makeElement('h4', [makeElement('span', name, null, {maxWidth: '200px'}), closeButton], {/*className: 'popover-title' */}, {paddingLeft: '10px', paddingRight: '10px'});

        var dragOX, dragOY;
        var moveHandler, upHandler;
        moveHandler = function(ev) {
            ev.stopPropagation(); ev.preventDefault();
            left = left + (ev.clientX - dragOX);
            if (left < 8) {
                left = 8;
            } if (left > (winWidth - width - 32)) {
                left = (winWidth - width - 26);
            }
            top = top + (ev.clientY - dragOY);
            top = Math.max(10, top);
            popup.style.top = '' + top + 'px';
            popup.style.left = '' + Math.min(left, (winWidth - width - 10)) + 'px';
            dragOX = ev.clientX; dragOY = ev.clientY;
        }
        upHandler = function(ev) {
            ev.stopPropagation(); ev.preventDefault();
            window.removeEventListener('mousemove', moveHandler, false);
            window.removeEventListener('mouseup', upHandler, false);
        }
        tbar.addEventListener('mousedown', function(ev) {
            ev.preventDefault(); ev.stopPropagation();
            dragOX = ev.clientX; dragOY = ev.clientY;
            window.addEventListener('mousemove', moveHandler, false);
            window.addEventListener('mouseup', upHandler, false);
        }, false);
                              

        popup.appendChild(tbar);
    }

    popup.appendChild(makeElement('div', ele, {className: 'popover-content'}, {
        padding: '0px'
    }));
    this.hPopupHolder.appendChild(popup);

    var popupHandle = {
        node: popup,
        displayed: true
    };
    popup.addEventListener('DOMNodeRemoved', function(ev) {
        if (ev.target == popup) {
            popupHandle.displayed = false;
        }
    }, false);
    return popupHandle;
}

function dlog(msg) {
    console.log(msg);
}
