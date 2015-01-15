(function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a=typeof require=="function"&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);throw new Error("Cannot find module '"+o+"'")}var f=n[o]={exports:{}};t[o][0].call(f.exports,function(e){var n=t[o][1][e];return s(n?n:e)},f,f.exports,e,t,n,r)}return n[o].exports}var i=typeof require=="function"&&require;for(var o=0;o<r.length;o++)s(r[o]);return s})({1:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// bam.js: indexed binary alignments
//

"use strict";

if (typeof(require) !== 'undefined') {
    var spans = require('./spans');
    var Range = spans.Range;
    var union = spans.union;
    var intersection = spans.intersection;

    var bin = require('./bin');
    var readInt = bin.readInt;
    var readShort = bin.readShort;
    var readByte = bin.readByte;
    var readInt64 = bin.readInt64;
    var readFloat = bin.readFloat;

    var lh3utils = require('./lh3utils');
    var readVob = lh3utils.readVob;
    var unbgzf = lh3utils.unbgzf;
    var reg2bins = lh3utils.reg2bins;
    var Chunk = lh3utils.Chunk;
}


var BAM_MAGIC = 0x14d4142;
var BAI_MAGIC = 0x1494142;

var BamFlags = {
    MULTIPLE_SEGMENTS:       0x1,
    ALL_SEGMENTS_ALIGN:      0x2,
    SEGMENT_UNMAPPED:        0x4,
    NEXT_SEGMENT_UNMAPPED:   0x8,
    REVERSE_COMPLEMENT:      0x10,
    NEXT_REVERSE_COMPLEMENT: 0x20,
    FIRST_SEGMENT:           0x40,
    LAST_SEGMENT:            0x80,
    SECONDARY_ALIGNMENT:     0x100,
    QC_FAIL:                 0x200,
    DUPLICATE:               0x400,
    SUPPLEMENTARY:           0x800
};

function BamFile() {
}


// Calculate the length (in bytes) of the BAI ref starting at offset.
// Returns {nbin, length, minBlockIndex}
function _getBaiRefLength(uncba, offset) {
    var p = offset;
    var nbin = readInt(uncba, p); p += 4;
    for (var b = 0; b < nbin; ++b) {
        var bin = readInt(uncba, p);
        var nchnk = readInt(uncba, p+4);
        p += 8 + (nchnk * 16);
    }
    var nintv = readInt(uncba, p); p += 4;

    var minBlockIndex = 1000000000;
    var q = p;
    for (var i = 0; i < nintv; ++i) {
        var v = readVob(uncba, q); q += 8;
        if (v) {
            var bi = v.block;
            if (v.offset > 0)
                bi += 65536;

            if (bi < minBlockIndex)
                minBlockIndex = bi;
            break;
        }
    }
    p += (nintv * 8);

    return {
        minBlockIndex: minBlockIndex,
        nbin: nbin,
        length: p - offset
    };
}


function makeBam(data, bai, indexChunks, callback) {
    var bam = new BamFile();
    bam.data = data;
    bam.bai = bai;
    bam.indexChunks = indexChunks;

    var minBlockIndex = bam.indexChunks ? bam.indexChunks.minBlockIndex : 1000000000;

    // Fills out bam.chrToIndex and bam.indexToChr based on the first few bytes of the BAM.
    function parseBamHeader(r) {
        if (!r) {
            return callback(null, "Couldn't access BAM");
        }

        var unc = unbgzf(r, r.byteLength);
        var uncba = new Uint8Array(unc);

        var magic = readInt(uncba, 0);
        if (magic != BAM_MAGIC) {
            return callback(null, "Not a BAM file, magic=0x" + magic.toString(16));
        }
        var headLen = readInt(uncba, 4);
        var header = '';
        for (var i = 0; i < headLen; ++i) {
            header += String.fromCharCode(uncba[i + 8]);
        }

        var nRef = readInt(uncba, headLen + 8);
        var p = headLen + 12;

        bam.chrToIndex = {};
        bam.indexToChr = [];
        for (var i = 0; i < nRef; ++i) {
            var lName = readInt(uncba, p);
            var name = '';
            for (var j = 0; j < lName-1; ++j) {
                name += String.fromCharCode(uncba[p + 4 + j]);
            }
            var lRef = readInt(uncba, p + lName + 4);
            bam.chrToIndex[name] = i;
            if (name.indexOf('chr') == 0) {
                bam.chrToIndex[name.substring(3)] = i;
            } else {
                bam.chrToIndex['chr' + name] = i;
            }
            bam.indexToChr.push(name);

            p = p + 8 + lName;
        }

        if (bam.indices) {
            return callback(bam);
        }
    }

    function parseBai(header) {
        if (!header) {
            return "Couldn't access BAI";
        }

        var uncba = new Uint8Array(header);
        var baiMagic = readInt(uncba, 0);
        if (baiMagic != BAI_MAGIC) {
            return callback(null, 'Not a BAI file, magic=0x' + baiMagic.toString(16));
        }

        var nref = readInt(uncba, 4);

        bam.indices = [];

        var p = 8;
        for (var ref = 0; ref < nref; ++ref) {
            var blockStart = p;
            var o = _getBaiRefLength(uncba, blockStart);
            p += o.length;

            minBlockIndex = Math.min(o.minBlockIndex, minBlockIndex);

            var nbin = o.nbin;

            if (nbin > 0) {
                bam.indices[ref] = new Uint8Array(header, blockStart, p - blockStart);
            }
        }

        return true;
    }

    if (!bam.indexChunks) {
        bam.bai.fetch(function(header) {   // Do we really need to fetch the whole thing? :-(
            var result = parseBai(header);
            if (result !== true) {
              callback(null, result);
            } else {
              bam.data.slice(0, minBlockIndex).fetch(parseBamHeader);
            }
        });
    } else {
        var chunks = bam.indexChunks.chunks;
        bam.indices = []
        for (var i = 0; i < chunks.length; i++) {
           bam.indices[i] = null;  // To be filled out lazily as needed
        }
        bam.data.slice(0, minBlockIndex).fetch(parseBamHeader);
    }
}



BamFile.prototype.blocksForRange = function(refId, min, max) {
    var index = this.indices[refId];
    if (!index) {
        return [];
    }

    var intBinsL = reg2bins(min, max);
    var intBins = [];
    for (var i = 0; i < intBinsL.length; ++i) {
        intBins[intBinsL[i]] = true;
    }
    var leafChunks = [], otherChunks = [];

    var nbin = readInt(index, 0);
    var p = 4;
    for (var b = 0; b < nbin; ++b) {
        var bin = readInt(index, p);
        var nchnk = readInt(index, p+4);
//        dlog('bin=' + bin + '; nchnk=' + nchnk);
        p += 8;
        if (intBins[bin]) {
            for (var c = 0; c < nchnk; ++c) {
                var cs = readVob(index, p);
                var ce = readVob(index, p + 8);
                (bin < 4681 ? otherChunks : leafChunks).push(new Chunk(cs, ce));
                p += 16;
            }
        } else {
            p +=  (nchnk * 16);
        }
    }
    // console.log('leafChunks = ' + miniJSONify(leafChunks));
    // console.log('otherChunks = ' + miniJSONify(otherChunks));

    var nintv = readInt(index, p);
    var lowest = null;
    var minLin = Math.min(min>>14, nintv - 1), maxLin = Math.min(max>>14, nintv - 1);
    for (var i = minLin; i <= maxLin; ++i) {
        var lb =  readVob(index, p + 4 + (i * 8));
        if (!lb) {
            continue;
        }
        if (!lowest || lb.block < lowest.block || lb.offset < lowest.offset) {
            lowest = lb;
        }
    }
    // console.log('Lowest LB = ' + lowest);
    
    var prunedOtherChunks = [];
    if (lowest != null) {
        for (var i = 0; i < otherChunks.length; ++i) {
            var chnk = otherChunks[i];
            if (chnk.maxv.block >= lowest.block && chnk.maxv.offset >= lowest.offset) {
                prunedOtherChunks.push(chnk);
            }
        }
    }
    // console.log('prunedOtherChunks = ' + miniJSONify(prunedOtherChunks));
    otherChunks = prunedOtherChunks;

    var intChunks = [];
    for (var i = 0; i < otherChunks.length; ++i) {
        intChunks.push(otherChunks[i]);
    }
    for (var i = 0; i < leafChunks.length; ++i) {
        intChunks.push(leafChunks[i]);
    }

    intChunks.sort(function(c0, c1) {
        var dif = c0.minv.block - c1.minv.block;
        if (dif != 0) {
            return dif;
        } else {
            return c0.minv.offset - c1.minv.offset;
        }
    });
    var mergedChunks = [];
    if (intChunks.length > 0) {
        var cur = intChunks[0];
        for (var i = 1; i < intChunks.length; ++i) {
            var nc = intChunks[i];
            if (nc.minv.block == cur.maxv.block /* && nc.minv.offset == cur.maxv.offset */) { // no point splitting mid-block
                cur = new Chunk(cur.minv, nc.maxv);
            } else {
                mergedChunks.push(cur);
                cur = nc;
            }
        }
        mergedChunks.push(cur);
    }
    // dlog('mergedChunks = ' + miniJSONify(mergedChunks));

    return mergedChunks;
}

BamFile.prototype.fetch = function(chr, min, max, callback, opts) {
    var thisB = this;
    opts = opts || {};

    var chrId = this.chrToIndex[chr];
    var chunks;
    if (chrId === undefined) {
        chunks = [];
    } else {
        // Fetch this portion of the BAI if it hasn't been loaded yet.
        if (this.indices[chrId] === null && this.indexChunks.chunks[chrId]) {
            var start_stop = this.indexChunks.chunks[chrId];
            return this.bai.slice(start_stop[0], start_stop[1]).fetch(function(data) {
                var buffer = new Uint8Array(data);
                this.indices[chrId] = buffer;
                return this.fetch(chr, min, max, callback, opts);
            }.bind(this));
        }

        chunks = this.blocksForRange(chrId, min, max);
        if (!chunks) {
            callback(null, 'Error in index fetch');
        }
    }
    
    var records = [];
    var index = 0;
    var data;

    function tramp() {
        if (index >= chunks.length) {
            return callback(records);
        } else if (!data) {
            var c = chunks[index];
            var fetchMin = c.minv.block;
            var fetchMax = c.maxv.block + (1<<16); // *sigh*
            thisB.data.slice(fetchMin, fetchMax - fetchMin).fetch(function(r) {
                data = unbgzf(r, c.maxv.block - c.minv.block + 1);
                return tramp();
            });
        } else {
            var ba = new Uint8Array(data);
            var finished = thisB.readBamRecords(ba, chunks[index].minv.offset, records, min, max, chrId, opts);
            data = null;
            ++index;
            if (finished)
                return callback(records);
            else
                return tramp();
        }
    }
    tramp();
}

var SEQRET_DECODER = ['=', 'A', 'C', 'x', 'G', 'x', 'x', 'x', 'T', 'x', 'x', 'x', 'x', 'x', 'x', 'N'];
var CIGAR_DECODER = ['M', 'I', 'D', 'N', 'S', 'H', 'P', '=', 'X', '?', '?', '?', '?', '?', '?', '?'];

function BamRecord() {
}

BamFile.prototype.readBamRecords = function(ba, offset, sink, min, max, chrId, opts) {
    while (true) {
        var blockSize = readInt(ba, offset);
        var blockEnd = offset + blockSize + 4;
        if (blockEnd >= ba.length) {
            return sink;
        }

        var record = new BamRecord();

        var refID = readInt(ba, offset + 4);
        var pos = readInt(ba, offset + 8);
        
        var bmn = readInt(ba, offset + 12);
        var bin = (bmn & 0xffff0000) >> 16;
        var mq = (bmn & 0xff00) >> 8;
        var nl = bmn & 0xff;

        var flag_nc = readInt(ba, offset + 16);
        var flag = (flag_nc & 0xffff0000) >> 16;
        var nc = flag_nc & 0xffff;
    
        var lseq = readInt(ba, offset + 20);
        
        var nextRef  = readInt(ba, offset + 24);
        var nextPos = readInt(ba, offset + 28);
        
        var tlen = readInt(ba, offset + 32);
    
        record.segment = this.indexToChr[refID];
        record.flag = flag;
        record.pos = pos;
        record.mq = mq;
        if (opts.light)
            record.seqLength = lseq;

        if (!opts.light) {
            if (nextRef >= 0) {
                record.nextSegment = this.indexToChr[nextRef];
                record.nextPos = nextPos;
            }

            var readName = '';
            for (var j = 0; j < nl-1; ++j) {
                readName += String.fromCharCode(ba[offset + 36 + j]);
            }
            record.readName = readName;
        
            var p = offset + 36 + nl;

            var cigar = '';
            for (var c = 0; c < nc; ++c) {
                var cigop = readInt(ba, p);
                cigar = cigar + (cigop>>4) + CIGAR_DECODER[cigop & 0xf];
                p += 4;
            }
            record.cigar = cigar;
        
            var seq = '';
            var seqBytes = (lseq + 1) >> 1;
            for (var j = 0; j < seqBytes; ++j) {
                var sb = ba[p + j];
                seq += SEQRET_DECODER[(sb & 0xf0) >> 4];
                seq += SEQRET_DECODER[(sb & 0x0f)];
            }
            p += seqBytes;
            record.seq = seq;

            var qseq = '';
            for (var j = 0; j < lseq; ++j) {
                qseq += String.fromCharCode(ba[p + j] + 33);
            }
            p += lseq;
            record.quals = qseq;

            while (p < blockEnd) {
                var tag = String.fromCharCode(ba[p], ba[p + 1]);
                var type = String.fromCharCode(ba[p + 2]);
                var value;

                if (type == 'A') {
                    value = String.fromCharCode(ba[p + 3]);
                    p += 4;
                } else if (type == 'i' || type == 'I') {
                    value = readInt(ba, p + 3);
                    p += 7;
                } else if (type == 'c' || type == 'C') {
                    value = ba[p + 3];
                    p += 4;
                } else if (type == 's' || type == 'S') {
                    value = readShort(ba, p + 3);
                    p += 5;
                } else if (type == 'f') {
                    value = readFloat(ba, p + 3);
                    p += 7;
                } else if (type == 'Z' || type == 'H') {
                    p += 3;
                    value = '';
                    for (;;) {
                        var cc = ba[p++];
                        if (cc == 0) {
                            break;
                        } else {
                            value += String.fromCharCode(cc);
                        }
                    }
                } else if (type == 'B') {
                    var atype = String.fromCharCode(ba[p + 3]);
                    var alen = readInt(ba, p + 4);
                    var elen;
                    var reader;
                    if (atype == 'i' || atype == 'I' || atype == 'f') {
                        elen = 4;
                        if (atype == 'f')
                            reader = readFloat;
                        else
                            reader = readInt;
                    } else if (atype == 's' || atype == 'S') {
                        elen = 2;
                        reader = readShort;
                    } else if (atype == 'c' || atype == 'C') {
                        elen = 1;
                        reader = readByte;
                    } else {
                        throw 'Unknown array type ' + atype;
                    }

                    p += 8;
                    value = [];
                    for (var i = 0; i < alen; ++i) {
                        value.push(reader(ba, p));
                        p += elen;
                    }
                } else {
                    throw 'Unknown type '+ type;
                }
                record[tag] = value;
            }
        }

        if (!min || record.pos <= max && record.pos + lseq >= min) {
            if (chrId === undefined || refID == chrId) {
                sink.push(record);
            }
        }
        if (record.pos > max) {
            return true;
        }
        offset = blockEnd;
    }

    // Exits via top of loop.
};

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeBam: makeBam,
        BAM_MAGIC: BAM_MAGIC,
        BAI_MAGIC: BAI_MAGIC,
        BamFlags: BamFlags
    };
}

},{"./bin":3,"./lh3utils":7,"./spans":9}],2:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// bigwig.js: indexed binary WIG (and BED) files
//

"use strict";


if (typeof(require) !== 'undefined') {
    var spans = require('./spans');
    var Range = spans.Range;
    var union = spans.union;
    var intersection = spans.intersection;

    var das = require('./das');
    var DASFeature = das.DASFeature;
    var DASGroup = das.DASGroup;

    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;

    var bin = require('./bin');
    var readInt = bin.readInt;

    var jszlib = require('jszlib');
    var jszlib_inflate_buffer = jszlib.inflateBuffer;
    var arrayCopy = jszlib.arrayCopy;
}

var BIG_WIG_MAGIC = 0x888FFC26;
var BIG_WIG_MAGIC_BE = 0x26FC8F88;
var BIG_BED_MAGIC = 0x8789F2EB;
var BIG_BED_MAGIC_BE = 0xEBF28987;


var BIG_WIG_TYPE_GRAPH = 1;
var BIG_WIG_TYPE_VSTEP = 2;
var BIG_WIG_TYPE_FSTEP = 3;
  
var M1 = 256;
var M2 = 256*256;
var M3 = 256*256*256;
var M4 = 256*256*256*256;

var BED_COLOR_REGEXP = new RegExp("^[0-9]+,[0-9]+,[0-9]+");

function bwg_readOffset(ba, o) {
    var offset = ba[o] + ba[o+1]*M1 + ba[o+2]*M2 + ba[o+3]*M3 + ba[o+4]*M4;
    return offset;
}

function BigWig() {
}

BigWig.prototype.readChromTree = function(callback) {
    var thisB = this;
    this.chromsToIDs = {};
    this.idsToChroms = {};
    this.maxID = 0;

    var udo = this.unzoomedDataOffset;
    var eb = (udo - this.chromTreeOffset) & 3;
    udo = udo + 4 - eb;

    this.data.slice(this.chromTreeOffset, udo - this.chromTreeOffset).fetch(function(bpt) {
        var ba = new Uint8Array(bpt);
        var sa = new Int16Array(bpt);
        var la = new Int32Array(bpt);
        var bptMagic = la[0];
        var blockSize = la[1];
        var keySize = la[2];
        var valSize = la[3];
        var itemCount = bwg_readOffset(ba, 16);
        var rootNodeOffset = 32;

        var bptReadNode = function(offset) {
            var nodeType = ba[offset];
            var cnt = sa[(offset/2) + 1];
            offset += 4;
            for (var n = 0; n < cnt; ++n) {
                if (nodeType == 0) {
                    offset += keySize;
                    var childOffset = bwg_readOffset(ba, offset);
                    offset += 8;
                    childOffset -= thisB.chromTreeOffset;
                    bptReadNode(childOffset);
                } else {
                    var key = '';
                    for (var ki = 0; ki < keySize; ++ki) {
                        var charCode = ba[offset++];
                        if (charCode != 0) {
                            key += String.fromCharCode(charCode);
                        }
                    }
                    var chromId = (ba[offset+3]<<24) | (ba[offset+2]<<16) | (ba[offset+1]<<8) | (ba[offset+0]);
                    var chromSize = (ba[offset + 7]<<24) | (ba[offset+6]<<16) | (ba[offset+5]<<8) | (ba[offset+4]);
                    offset += 8;

                    thisB.chromsToIDs[key] = chromId;
                    if (key.indexOf('chr') == 0) {
                        thisB.chromsToIDs[key.substr(3)] = chromId;
                    }
                    thisB.idsToChroms[chromId] = key;
                    thisB.maxID = Math.max(thisB.maxID, chromId);
                }
            }
        };
        bptReadNode(rootNodeOffset);

        callback(thisB);
    });
}

function BigWigView(bwg, cirTreeOffset, cirTreeLength, isSummary) {
    this.bwg = bwg;
    this.cirTreeOffset = cirTreeOffset;
    this.cirTreeLength = cirTreeLength;
    this.isSummary = isSummary;
}



BigWigView.prototype.readWigData = function(chrName, min, max, callback) {
    var chr = this.bwg.chromsToIDs[chrName];
    if (chr === undefined) {
        // Not an error because some .bwgs won't have data for all chromosomes.
        return callback([]);
    } else {
        this.readWigDataById(chr, min, max, callback);
    }
}

BigWigView.prototype.readWigDataById = function(chr, min, max, callback) {
    var thisB = this;
    if (!this.cirHeader) {
        this.bwg.data.slice(this.cirTreeOffset, 48).fetch(function(result) {
            thisB.cirHeader = result;
            var la = new Int32Array(thisB.cirHeader);
            thisB.cirBlockSize = la[1];
            thisB.readWigDataById(chr, min, max, callback);
        });
        return;
    }

    var blocksToFetch = [];
    var outstanding = 0;

    var beforeBWG = Date.now();

    var filter = function(chromId, fmin, fmax, toks) {
        return ((chr < 0 || chromId == chr) && fmin <= max && fmax >= min);
    }

    var cirFobRecur = function(offset, level) {
        if (thisB.bwg.instrument)
            console.log('level=' + level + '; offset=' + offset + '; time=' + (Date.now()|0));

        outstanding += offset.length;

        if (offset.length == 1 && offset[0] - thisB.cirTreeOffset == 48 && thisB.cachedCirRoot) {
            cirFobRecur2(thisB.cachedCirRoot, 0, level);
            --outstanding;
            if (outstanding == 0) {
                thisB.fetchFeatures(filter, blocksToFetch, callback);
            }
            return;
        }

        var maxCirBlockSpan = 4 +  (thisB.cirBlockSize * 32);   // Upper bound on size, based on a completely full leaf node.
        var spans;
        for (var i = 0; i < offset.length; ++i) {
            var blockSpan = new Range(offset[i], offset[i] + maxCirBlockSpan);
            spans = spans ? union(spans, blockSpan) : blockSpan;
        }
        
        var fetchRanges = spans.ranges();
        for (var r = 0; r < fetchRanges.length; ++r) {
            var fr = fetchRanges[r];
            cirFobStartFetch(offset, fr, level);
        }
    }

    var cirFobStartFetch = function(offset, fr, level, attempts) {
        var length = fr.max() - fr.min();
        thisB.bwg.data.slice(fr.min(), fr.max() - fr.min()).fetch(function(resultBuffer) {
            for (var i = 0; i < offset.length; ++i) {
                if (fr.contains(offset[i])) {
                    cirFobRecur2(resultBuffer, offset[i] - fr.min(), level);

                    if (offset[i] - thisB.cirTreeOffset == 48 && offset[i] - fr.min() == 0)
                        thisB.cachedCirRoot = resultBuffer;

                    --outstanding;
                    if (outstanding == 0) {
                        thisB.fetchFeatures(filter, blocksToFetch, callback);
                    }
                }
            }
        });
    }

    var cirFobRecur2 = function(cirBlockData, offset, level) {
        var ba = new Uint8Array(cirBlockData);
        var sa = new Int16Array(cirBlockData);
        var la = new Int32Array(cirBlockData);

        var isLeaf = ba[offset];
        var cnt = sa[offset/2 + 1];
        offset += 4;

        if (isLeaf != 0) {
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                var blockSize = bwg_readOffset(ba, offset+24);
                if (((chr < 0 || startChrom < chr) || (startChrom == chr && startBase <= max)) &&
                    ((chr < 0 || endChrom   > chr) || (endChrom == chr && endBase >= min)))
                {
                    blocksToFetch.push({offset: blockOffset, size: blockSize});
                }
                offset += 32;
            }
        } else {
            var recurOffsets = [];
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                if ((chr < 0 || startChrom < chr || (startChrom == chr && startBase <= max)) &&
                    (chr < 0 || endChrom   > chr || (endChrom == chr && endBase >= min)))
                {
                    recurOffsets.push(blockOffset);
                }
                offset += 24;
            }
            if (recurOffsets.length > 0) {
                cirFobRecur(recurOffsets, level + 1);
            }
        }
    };

    cirFobRecur([thisB.cirTreeOffset + 48], 1);
}


BigWigView.prototype.fetchFeatures = function(filter, blocksToFetch, callback) {
    var thisB = this;

    blocksToFetch.sort(function(b0, b1) {
        return (b0.offset|0) - (b1.offset|0);
    });

    if (blocksToFetch.length == 0) {
        callback([]);
    } else {
        var features = [];
        var createFeature = function(chr, fmin, fmax, opts) {
            if (!opts) {
                opts = {};
            }
        
            var f = new DASFeature();
            f._chromId = chr;
            f.segment = thisB.bwg.idsToChroms[chr];
            f.min = fmin;
            f.max = fmax;
            f.type = 'bigwig';
            
            for (var k in opts) {
                f[k] = opts[k];
            }
            
            features.push(f);
        };

        var tramp = function() {
            if (blocksToFetch.length == 0) {
                var afterBWG = Date.now();
                // dlog('BWG fetch took ' + (afterBWG - beforeBWG) + 'ms');
                callback(features);
                return;  // just in case...
            } else {
                var block = blocksToFetch[0];
                if (block.data) {
                    thisB.parseFeatures(block.data, createFeature, filter);
                    blocksToFetch.splice(0, 1);
                    tramp();
                } else {
                    var fetchStart = block.offset;
                    var fetchSize = block.size;
                    var bi = 1;
                    while (bi < blocksToFetch.length && blocksToFetch[bi].offset == (fetchStart + fetchSize)) {
                        fetchSize += blocksToFetch[bi].size;
                        ++bi;
                    }

                    thisB.bwg.data.slice(fetchStart, fetchSize).fetch(function(result) {
                        var offset = 0;
                        var bi = 0;
                        while (offset < fetchSize) {
                            var fb = blocksToFetch[bi];
                        
                            var data;
                            if (thisB.bwg.uncompressBufSize > 0) {
                                data = jszlib_inflate_buffer(result, offset + 2, fb.size - 2);
                            } else {
                                var tmp = new Uint8Array(fb.size);    // FIXME is this really the best we can do?
                                arrayCopy(new Uint8Array(result, offset, fb.size), 0, tmp, 0, fb.size);
                                data = tmp.buffer;
                            }
                            fb.data = data;
                            
                            offset += fb.size;
                            ++bi;
                        }
                        tramp();
                    });
                }
            }
        }
        tramp();
    }
}

BigWigView.prototype.parseFeatures = function(data, createFeature, filter) {
    var ba = new Uint8Array(data);

    if (this.isSummary) {
        var sa = new Int16Array(data);
        var la = new Int32Array(data);
        var fa = new Float32Array(data);

        var itemCount = data.byteLength/32;
        for (var i = 0; i < itemCount; ++i) {
            var chromId =   la[(i*8)];
            var start =     la[(i*8)+1];
            var end =       la[(i*8)+2];
            var validCnt =  la[(i*8)+3];
            var minVal    = fa[(i*8)+4];
            var maxVal    = fa[(i*8)+5];
            var sumData   = fa[(i*8)+6];
            var sumSqData = fa[(i*8)+7];
            
            if (filter(chromId, start + 1, end)) {
                var summaryOpts = {type: 'bigwig', score: sumData/validCnt, maxScore: maxVal};
                if (this.bwg.type == 'bigbed') {
                    summaryOpts.type = 'density';
                }
                createFeature(chromId, start + 1, end, summaryOpts);
            }
        }
    } else if (this.bwg.type == 'bigwig') {
        var sa = new Int16Array(data);
        var la = new Int32Array(data);
        var fa = new Float32Array(data);

        var chromId = la[0];
        var blockStart = la[1];
        var blockEnd = la[2];
        var itemStep = la[3];
        var itemSpan = la[4];
        var blockType = ba[20];
        var itemCount = sa[11];
        
        if (blockType == BIG_WIG_TYPE_FSTEP) {
            for (var i = 0; i < itemCount; ++i) {
                var score = fa[i + 6];
                var fmin = blockStart + (i*itemStep) + 1, fmax = blockStart + (i*itemStep) + itemSpan;
                if (filter(chromId, fmin, fmax))
                    createFeature(chromId, fmin, fmax, {score: score});
            }
        } else if (blockType == BIG_WIG_TYPE_VSTEP) {
            for (var i = 0; i < itemCount; ++i) {
                var start = la[(i*2) + 6] + 1;
                var end = start + itemSpan - 1;
                var score = fa[(i*2) + 7];
                if (filter(chromId, start, end))
                    createFeature(chromId, start, end, {score: score});
            }
        } else if (blockType == BIG_WIG_TYPE_GRAPH) {
            for (var i = 0; i < itemCount; ++i) {
                var start = la[(i*3) + 6] + 1;
                var end   = la[(i*3) + 7];
                var score = fa[(i*3) + 8];
                if (start > end) {
                    start = end;
                }
                if (filter(chromId, start, end))
                    createFeature(chromId, start, end, {score: score});
            }
        } else {
            console.log('Currently not handling bwgType=' + blockType);
        }
    } else if (this.bwg.type == 'bigbed') {
        var offset = 0;
        var dfc = this.bwg.definedFieldCount;
        var schema = this.bwg.schema;

        while (offset < ba.length) {
            var chromId = (ba[offset+3]<<24) | (ba[offset+2]<<16) | (ba[offset+1]<<8) | (ba[offset+0]);
            var start = (ba[offset+7]<<24) | (ba[offset+6]<<16) | (ba[offset+5]<<8) | (ba[offset+4]);
            var end = (ba[offset+11]<<24) | (ba[offset+10]<<16) | (ba[offset+9]<<8) | (ba[offset+8]);
            offset += 12;
            var rest = '';
            while (true) {
                var ch = ba[offset++];
                if (ch != 0) {
                    rest += String.fromCharCode(ch);
                } else {
                    break;
                }
            }

            var featureOpts = {};
            
            var bedColumns;
            if (rest.length > 0) {
                bedColumns = rest.split('\t');
            } else {
                bedColumns = [];
            }
            if (bedColumns.length > 0 && dfc > 3) {
                featureOpts.label = bedColumns[0];
            }
            if (bedColumns.length > 1 && dfc > 4) {
                var score = parseInt(bedColumns[1]);
                if (!isNaN(score))
                    featureOpts.score = score;
            }
            if (bedColumns.length > 2 && dfc > 5) {
                featureOpts.orientation = bedColumns[2];
            }
            if (bedColumns.length > 5 && dfc > 8) {
                var color = bedColumns[5];
                if (BED_COLOR_REGEXP.test(color)) {
                    featureOpts.itemRgb = 'rgb(' + color + ')';
                }
            }

            if (bedColumns.length > dfc-3 && schema) {
                for (var col = dfc - 3; col < bedColumns.length; ++col) {
                    featureOpts[schema.fields[col+3].name] = bedColumns[col];
                }
            }

            if (filter(chromId, start + 1, end, bedColumns)) {
                if (dfc < 12) {
                    createFeature(chromId, start + 1, end, featureOpts);
                } else {
                    var thickStart = bedColumns[3]|0;
                    var thickEnd   = bedColumns[4]|0;
                    var blockCount = bedColumns[6]|0;
                    var blockSizes = bedColumns[7].split(',');
                    var blockStarts = bedColumns[8].split(',');

                    if (featureOpts.exonFrames) {
                        var exonFrames = featureOpts.exonFrames.split(',');
                        featureOpts.exonFrames = undefined;
                    }
                    
                    featureOpts.type = 'transcript'
                    var grp = new DASGroup();
                    for (var k in featureOpts) {
                        grp[k] = featureOpts[k];
                    }
                    grp.id = bedColumns[0];
                    grp.segment = this.bwg.idsToChroms[chromId];
                    grp.min = start + 1;
                    grp.max = end;
                    grp.notes = [];
                    featureOpts.groups = [grp];

                    // Moving towards using bigGenePred model, but will
                    // still support old Dalliance-style BED12+gene-name for the
                    // foreseeable future.
                    if (bedColumns.length > 9) {
                        var geneId = featureOpts.geneName || bedColumns[9];
                        var geneName = geneId;
                        if (bedColumns.length > 10) {
                            geneName = bedColumns[10];
                        }
                        if (featureOpts.geneName2)
                            geneName = featureOpts.geneName2;

                        var gg = shallowCopy(grp);
                        gg.id = geneId;
                        gg.label = geneName;
                        gg.type = 'gene';
                        featureOpts.groups.push(gg);
                    }

                    var spanList = [];
                    for (var b = 0; b < blockCount; ++b) {
                        var bmin = (blockStarts[b]|0) + start;
                        var bmax = bmin + (blockSizes[b]|0);
                        var span = new Range(bmin, bmax);
                        spanList.push(span);
                    }
                    var spans = union(spanList);
                    
                    var tsList = spans.ranges();
                    for (var s = 0; s < tsList.length; ++s) {
                        var ts = tsList[s];
                        createFeature(chromId, ts.min() + 1, ts.max(), featureOpts);
                    }

                    if (thickEnd > thickStart) {
                        var codingRegion = (featureOpts.orientation == '+') ?
                            new Range(thickStart, thickEnd + 3) :
                            new Range(thickStart - 3, thickEnd);
                            // +/- 3 to account for stop codon

                        var tl = intersection(spans, codingRegion);
                        if (tl) {
                            featureOpts.type = 'translation';
                            var tlList = tl.ranges();
                            var readingFrame = 0;

                            var tlOffset = 0;
                            while (tlList[0].min() > tsList[tlOffset].max())
                                tlOffset++;

                            for (var s = 0; s < tlList.length; ++s) {
                                // Record reading frame for every exon
                                var index = s;
                                if (featureOpts.orientation == '-')
                                    index = tlList.length - s - 1;
                                var ts = tlList[index];
                                featureOpts.readframe = readingFrame;
                                if (exonFrames) {
                                    var brf = parseInt(exonFrames[index + tlOffset]);
                                    if (typeof(brf) === 'number' && brf >= 0 && brf <= 2) {
                                        featureOpts.readframe = brf;
                                        featureOpts.readframeExplicit = true;
                                    }
                                }
                                var length = ts.max() - ts.min();
                                readingFrame = (readingFrame + length) % 3;
                                createFeature(chromId, ts.min() + 1, ts.max(), featureOpts);
                            }
                        }
                    }
                }
            }
        }
    } else {
        throw Error("Don't know what to do with " + this.bwg.type);
    }
}

//
// nasty cut/paste, should roll back in!
//

BigWigView.prototype.getFirstAdjacent = function(chrName, pos, dir, callback) {
    var chr = this.bwg.chromsToIDs[chrName];
    if (chr === undefined) {
        // Not an error because some .bwgs won't have data for all chromosomes.
        return callback([]);
    } else {
        this.getFirstAdjacentById(chr, pos, dir, callback);
    }
}

BigWigView.prototype.getFirstAdjacentById = function(chr, pos, dir, callback) {
    var thisB = this;
    if (!this.cirHeader) {
        this.bwg.data.slice(this.cirTreeOffset, 48).fetch(function(result) {
            thisB.cirHeader = result;
            var la = new Int32Array(thisB.cirHeader);
            thisB.cirBlockSize = la[1];
            thisB.getFirstAdjacentById(chr, pos, dir, callback);
        });
        return;
    }

    var blockToFetch = null;
    var bestBlockChr = -1;
    var bestBlockOffset = -1;

    var outstanding = 0;

    var beforeBWG = Date.now();

    var cirFobRecur = function(offset, level) {
        outstanding += offset.length;

        var maxCirBlockSpan = 4 +  (thisB.cirBlockSize * 32);   // Upper bound on size, based on a completely full leaf node.
        var spans;
        for (var i = 0; i < offset.length; ++i) {
            var blockSpan = new Range(offset[i], offset[i] + maxCirBlockSpan);
            spans = spans ? union(spans, blockSpan) : blockSpan;
        }
        
        var fetchRanges = spans.ranges();
        for (var r = 0; r < fetchRanges.length; ++r) {
            var fr = fetchRanges[r];
            cirFobStartFetch(offset, fr, level);
        }
    }

    var cirFobStartFetch = function(offset, fr, level, attempts) {
        var length = fr.max() - fr.min();
        thisB.bwg.data.slice(fr.min(), fr.max() - fr.min()).fetch(function(resultBuffer) {
            for (var i = 0; i < offset.length; ++i) {
                if (fr.contains(offset[i])) {
                    cirFobRecur2(resultBuffer, offset[i] - fr.min(), level);
                    --outstanding;
                    if (outstanding == 0) {
                        if (!blockToFetch) {
                            if (dir > 0 && (chr != 0 || pos > 0)) {
                                return thisB.getFirstAdjacentById(0, 0, dir, callback);
                            } else if (dir < 0 && (chr != thisB.bwg.maxID || pos < 1000000000)) {
                                return thisB.getFirstAdjacentById(thisB.bwg.maxID, 1000000000, dir, callback);
                            }
                            return callback([]);
                        }

                        thisB.fetchFeatures(function(chrx, fmin, fmax, toks) {
                            return (dir < 0 && (chrx < chr || fmax < pos)) || (dir > 0 && (chrx > chr || fmin > pos));
                        }, [blockToFetch], function(features) {
                            var bestFeature = null;
                            var bestChr = -1;
                            var bestPos = -1;
                            for (var fi = 0; fi < features.length; ++fi) {
                                var f = features[fi];
                                var chrx = f._chromId, fmin = f.min, fmax = f.max;
                                if (bestFeature == null || ((dir < 0) && (chrx > bestChr || fmax > bestPos)) || ((dir > 0) && (chrx < bestChr || fmin < bestPos))) {
                                    bestFeature = f;
                                    bestPos = (dir < 0) ? fmax : fmin;
                                    bestChr = chrx;
                                }
                            }

                            if (bestFeature != null) 
                                return callback([bestFeature]);
                            else
                                return callback([]);
                        });
                    }
                }
            }
        });
    }

    var cirFobRecur2 = function(cirBlockData, offset, level) {
        var ba = new Uint8Array(cirBlockData);
        var sa = new Int16Array(cirBlockData);
        var la = new Int32Array(cirBlockData);

        var isLeaf = ba[offset];
        var cnt = sa[offset/2 + 1];
        offset += 4;

        if (isLeaf != 0) {
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = bwg_readOffset(ba, offset+16);
                var blockSize = bwg_readOffset(ba, offset+24);
                if ((dir < 0 && ((startChrom < chr || (startChrom == chr && startBase <= pos)))) ||
                    (dir > 0 && ((endChrom > chr || (endChrom == chr && endBase >= pos)))))
                {
                    // console.log('Got an interesting block: startBase=' + startChrom + ':' + startBase + '; endBase=' + endChrom + ':' + endBase + '; offset=' + blockOffset + '; size=' + blockSize);
                    if (/_random/.exec(thisB.bwg.idsToChroms[startChrom])) {
                        // dlog('skipping random: ' + thisB.bwg.idsToChroms[startChrom]);
                    } else if (blockToFetch == null || ((dir < 0) && (endChrom > bestBlockChr || (endChrom == bestBlockChr && endBase > bestBlockOffset)) ||
                                                 (dir > 0) && (startChrom < bestBlockChr || (startChrom == bestBlockChr && startBase < bestBlockOffset))))
                    {
                        //                        dlog('best is: startBase=' + startChrom + ':' + startBase + '; endBase=' + endChrom + ':' + endBase + '; offset=' + blockOffset + '; size=' + blockSize);
                        blockToFetch = {offset: blockOffset, size: blockSize};
                        bestBlockOffset = (dir < 0) ? endBase : startBase;
                        bestBlockChr = (dir < 0) ? endChrom : startChrom;
                    }
                }
                offset += 32;
            }
        } else {
            var bestRecur = -1;
            var bestPos = -1;
            var bestChr = -1;
            for (var i = 0; i < cnt; ++i) {
                var lo = offset/4;
                var startChrom = la[lo];
                var startBase = la[lo + 1];
                var endChrom = la[lo + 2];
                var endBase = la[lo + 3];
                var blockOffset = (la[lo + 4]<<32) | (la[lo + 5]);
                if ((dir < 0 && ((startChrom < chr || (startChrom == chr && startBase <= pos)) &&
                                 (endChrom   >= chr))) ||
                     (dir > 0 && ((endChrom > chr || (endChrom == chr && endBase >= pos)) &&
                                  (startChrom <= chr))))
                {
                    if (bestRecur < 0 || endBase > bestPos) {
                        bestRecur = blockOffset;
                        bestPos = (dir < 0) ? endBase : startBase;
                        bestChr = (dir < 0) ? endChrom : startChrom;
                    }
                }
                offset += 24;
            }
            if (bestRecur >= 0) {
                cirFobRecur([bestRecur], level + 1);
            }
        }
    };
    

    cirFobRecur([thisB.cirTreeOffset + 48], 1);
}

BigWig.prototype.readWigData = function(chrName, min, max, callback) {
    this.getUnzoomedView().readWigData(chrName, min, max, callback);
}

BigWig.prototype.getUnzoomedView = function() {
    if (!this.unzoomedView) {
        var cirLen = 4000;
        var nzl = this.zoomLevels[0];
        if (nzl) {
            cirLen = this.zoomLevels[0].dataOffset - this.unzoomedIndexOffset;
        }
        this.unzoomedView = new BigWigView(this, this.unzoomedIndexOffset, cirLen, false);
    }
    return this.unzoomedView;
}

BigWig.prototype.getZoomedView = function(z) {
    var zh = this.zoomLevels[z];
    if (!zh.view) {
        zh.view = new BigWigView(this, zh.indexOffset, /* this.zoomLevels[z + 1].dataOffset - zh.indexOffset */ 4000, true);
    }
    return zh.view;
}

function makeBwg(data, callback, name) {
    var bwg = new BigWig();
    bwg.data = data;
    bwg.name = name;
    bwg.data.slice(0, 512).salted().fetch(function(result) {
        if (!result) {
            return callback(null, "Couldn't fetch file");
        }

        var header = result;
        var ba = new Uint8Array(header);
        var sa = new Int16Array(header);
        var la = new Int32Array(header);
        var magic = ba[0] + (M1 * ba[1]) + (M2 * ba[2]) + (M3 * ba[3]);
        if (magic == BIG_WIG_MAGIC) {
            bwg.type = 'bigwig';
        } else if (magic == BIG_BED_MAGIC) {
            bwg.type = 'bigbed';
        } else if (magic == BIG_WIG_MAGIC_BE || magic == BIG_BED_MAGIC_BE) {
            callback(null, "Currently don't support big-endian BBI files");
        } else {
            callback(null, "Not a supported format, magic=0x" + magic.toString(16));
        }

        bwg.version = sa[2];             // 4
        bwg.numZoomLevels = sa[3];       // 6
        bwg.chromTreeOffset = bwg_readOffset(ba, 8);
        bwg.unzoomedDataOffset = bwg_readOffset(ba, 16);
        bwg.unzoomedIndexOffset = bwg_readOffset(ba, 24);
        bwg.fieldCount = sa[16];         // 32
        bwg.definedFieldCount = sa[17];  // 34
        bwg.asOffset = bwg_readOffset(ba, 36);
        bwg.totalSummaryOffset = bwg_readOffset(ba, 44);
        bwg.uncompressBufSize = la[13];  // 52
        bwg.extHeaderOffset = bwg_readOffset(ba, 56);

        bwg.zoomLevels = [];
        for (var zl = 0; zl < bwg.numZoomLevels; ++zl) {
            var zlReduction = la[zl*6 + 16]
            var zlData = bwg_readOffset(ba, zl*24 + 72);
            var zlIndex = bwg_readOffset(ba, zl*24 + 80);
            bwg.zoomLevels.push({reduction: zlReduction, dataOffset: zlData, indexOffset: zlIndex});
        }

        bwg.readChromTree(function() {
            bwg.getAutoSQL(function(as) {
                bwg.schema = as;
                return callback(bwg);
            });
        });
    });
}


BigWig.prototype._tsFetch = function(zoom, chr, min, max, callback) {
    var bwg = this;
    if (zoom >= this.zoomLevels.length - 1) {
        if (!this.topLevelReductionCache) {
            this.getZoomedView(this.zoomLevels.length - 1).readWigDataById(-1, 0, 300000000, function(feats) {
                bwg.topLevelReductionCache = feats;
                return bwg._tsFetch(zoom, chr, min, max, callback);
            });
        } else {
            var f = [];
            var c = this.topLevelReductionCache;
            for (var fi = 0; fi < c.length; ++fi) {
                if (c[fi]._chromId == chr) {
                    f.push(c[fi]);
                }
            }
            return callback(f);
        }
    } else {
        var view;
        if (zoom < 0) {
            view = this.getUnzoomedView();
        } else {
            view = this.getZoomedView(zoom);
        }
        return view.readWigDataById(chr, min, max, callback);
    }
}

BigWig.prototype.thresholdSearch = function(chrName, referencePoint, dir, threshold, callback) {
    dir = (dir<0) ? -1 : 1;
    var bwg = this;
    var initialChr = this.chromsToIDs[chrName];
    var candidates = [{chrOrd: 0, chr: initialChr, zoom: bwg.zoomLevels.length - 4, min: 0, max: 300000000, fromRef: true}]
    for (var i = 1; i <= this.maxID + 1; ++i) {
        var chrId = (initialChr + (dir*i)) % (this.maxID + 1);
        if (chrId < 0) 
            chrId += (this.maxID + 1);
        candidates.push({chrOrd: i, chr: chrId, zoom: bwg.zoomLevels.length - 1, min: 0, max: 300000000})
    }
       
    function fbThresholdSearchRecur() {
    	if (candidates.length == 0) {
    	    return callback(null);
    	}
    	candidates.sort(function(c1, c2) {
    	    var d = c1.zoom - c2.zoom;
    	    if (d != 0)
    		    return d;

            d = c1.chrOrd - c2.chrOrd;
            if (d != 0)
                return d;
    	    else
    		    return c1.min - c2.min * dir;
    	});

	    var candidate = candidates.splice(0, 1)[0];
        bwg._tsFetch(candidate.zoom, candidate.chr, candidate.min, candidate.max, function(feats) {
            var rp = dir > 0 ? 0 : 300000000;
            if (candidate.fromRef)
                rp = referencePoint;
            
            for (var fi = 0; fi < feats.length; ++fi) {
    	        var f = feats[fi];
                var score;
                if (f.maxScore != undefined)
                    score = f.maxScore;
                else
                    score = f.score;

                if (dir > 0) {
    	            if (score > threshold) {
        		        if (candidate.zoom < 0) {
        		            if (f.min > rp)
                                return callback(f);
        		        } else if (f.max > rp) {
        		            candidates.push({chr: candidate.chr, chrOrd: candidate.chrOrd, zoom: candidate.zoom - 2, min: f.min, max: f.max, fromRef: candidate.fromRef});
        		        }
                    }
                } else {
                    if (score > threshold) {
            		    if (candidate.zoom < 0) {
                	        if (f.max < rp)
                			    return callback(f);
                        } else if (f.min < rp) {
                            candidates.push({chr: candidate.chr, chrOrd: candidate.chrOrd, zoom: candidate.zoom - 2, min: f.min, max: f.max, fromRef: candidate.fromRef});
                        }
    	            }
                }
    	    }
            fbThresholdSearchRecur();
        });
    }
    
    fbThresholdSearchRecur();
}

BigWig.prototype.getAutoSQL = function(callback) {
    var thisB = this;
    if (!this.asOffset)
        return callback(null);


    this.data.slice(this.asOffset, 2048).fetch(function(result) {
        var ba = new Uint8Array(result);
        var s = '';
        for (var i = 0; i < ba.length; ++i) {
            if (ba[i] == 0)
                break;
            s += String.fromCharCode(ba[i]);
        }
        
        /* 
         * Quick'n'dirty attempt to parse autoSql format.
         * See: http://www.linuxjournal.com/files/linuxjournal.com/linuxjournal/articles/059/5949/5949l2.html
         */

        var header_re = /(\w+)\s+(\w+)\s+("([^"]+)")?\s+\(\s*/;
        var field_re = /([\w\[\]]+)\s+(\w+)\s*;\s*("([^"]+)")?\s*/g;

        var headerMatch = header_re.exec(s);
        if (headerMatch) {
            var as = {
                declType: headerMatch[1],
                name: headerMatch[2],
                comment: headerMatch[4],

                fields: []
            };

            s = s.substring(headerMatch[0]);
            for (var m = field_re.exec(s); m != null; m = field_re.exec(s)) {
                as.fields.push({type: m[1],
                             name: m[2],
                             comment: m[4]});
            }

            return callback(as);
        }
    });
}

BigWig.prototype.getExtraIndices = function(callback) {
    var thisB = this;
    if (this.version < 4 || this.extHeaderOffset == 0 || this.type != 'bigbed') {
        return callback(null);
    } else {
        this.data.slice(this.extHeaderOffset, 64).fetch(function(result) {
            if (!result) {
                return callback(null, "Couldn't fetch extension header");
            }

            var ba = new Uint8Array(result);
            var sa = new Int16Array(result);
            var la = new Int32Array(result);
            
            var extHeaderSize = sa[0];
            var extraIndexCount = sa[1];
            var extraIndexListOffset = bwg_readOffset(ba, 4);

            if (extraIndexCount == 0) {
                return callback(null);
            }

            // FIXME 20byte records only make sense for single-field indices.
            // Right now, these seem to be the only things around, but the format
            // is actually more general.
            thisB.data.slice(extraIndexListOffset, extraIndexCount * 20).fetch(function(eil) {
                if (!eil) {
                    return callback(null, "Couldn't fetch index info");
                }

                var ba = new Uint8Array(eil);
                var sa = new Int16Array(eil);
                var la = new Int32Array(eil);

                var indices = [];
                for (var ii = 0; ii < extraIndexCount; ++ii) {
                    var eiType = sa[ii*10];
                    var eiFieldCount = sa[ii*10 + 1];
                    var eiOffset = bwg_readOffset(ba, ii*20 + 4);
                    var eiField = sa[ii*10 + 8]
                    var index = new BBIExtraIndex(thisB, eiType, eiFieldCount, eiOffset, eiField);
                    indices.push(index);
                }
                callback(indices);
            });
        });
    }
}

function BBIExtraIndex(bbi, type, fieldCount, offset, field) {
    this.bbi = bbi;
    this.type = type;
    this.fieldCount = fieldCount;
    this.offset = offset;
    this.field = field;
}

BBIExtraIndex.prototype.lookup = function(name, callback) {
    var thisB = this;

    this.bbi.data.slice(this.offset, 32).fetch(function(bpt) {
        var ba = new Uint8Array(bpt);
        var sa = new Int16Array(bpt);
        var la = new Int32Array(bpt);
        var bptMagic = la[0];
        var blockSize = la[1];
        var keySize = la[2];
        var valSize = la[3];
        var itemCount = bwg_readOffset(ba, 16);
        var rootNodeOffset = 32;

        function bptReadNode(nodeOffset) {
            thisB.bbi.data.slice(nodeOffset, 4 + (blockSize * (keySize + valSize))).fetch(function(node) {
                var ba = new Uint8Array(node);
                var sa = new Uint16Array(node);
                var la = new Uint32Array(node);

                var nodeType = ba[0];
                var cnt = sa[1];

                var offset = 4;
                if (nodeType == 0) {
                    var lastChildOffset = null;
                    for (var n = 0; n < cnt; ++n) {
                        var key = '';
                        for (var ki = 0; ki < keySize; ++ki) {
                            var charCode = ba[offset++];
                            if (charCode != 0) {
                                key += String.fromCharCode(charCode);
                            }
                        }

                        var childOffset = bwg_readOffset(ba, offset);
                        offset += 8;
                        
                        if (name.localeCompare(key) < 0 && lastChildOffset) {
                            bptReadNode(lastChildOffset);
                            return;
                        }
                        lastChildOffset = childOffset;
                    }
                    bptReadNode(lastChildOffset);
                } else {
                    for (var n = 0; n < cnt; ++n) {
                        var key = '';
                        for (var ki = 0; ki < keySize; ++ki) {
                            var charCode = ba[offset++];
                            if (charCode != 0) {
                                key += String.fromCharCode(charCode);
                            }
                        }
                        
                        // Specific for EI case.
                        if (key == name) {
                            var start = bwg_readOffset(ba, offset);
                            var length = readInt(ba, offset + 8);

                            return thisB.bbi.getUnzoomedView().fetchFeatures(
                                function(chr, min, max, toks) {
                                    if (toks && toks.length > thisB.field - 3)
                                        return toks[thisB.field - 3] == name;
                                }, 
                                [{offset: start, size: length}], 
                                callback);
                        }
                        offset += valSize;
                    }
                    return callback([]);
                }
            });
        }

        bptReadNode(thisB.offset + rootNodeOffset);
    });
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeBwg: makeBwg,
        BIG_BED_MAGIC: BIG_BED_MAGIC,
        BIG_WIG_MAGIC: BIG_WIG_MAGIC
    }
}

},{"./bin":3,"./das":5,"./spans":9,"./utils":10,"jszlib":11}],3:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// bin.js general binary data support
//

"use strict";

if (typeof(require) !== 'undefined') {
    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;

    var sha1 = require('./sha1');
    var b64_sha1 = sha1.b64_sha1;
}

function BlobFetchable(b) {
    this.blob = b;
}

BlobFetchable.prototype.slice = function(start, length) {
    var b;

    if (this.blob.slice) {
        if (length) {
            b = this.blob.slice(start, start + length);
        } else {
            b = this.blob.slice(start);
        }
    } else {
        if (length) {
            b = this.blob.webkitSlice(start, start + length);
        } else {
            b = this.blob.webkitSlice(start);
        }
    }
    return new BlobFetchable(b);
}

BlobFetchable.prototype.salted = function() {return this;}

if (typeof(FileReader) !== 'undefined') {
    // console.log('defining async BlobFetchable.fetch');

    BlobFetchable.prototype.fetch = function(callback) {
        var reader = new FileReader();
        reader.onloadend = function(ev) {
            callback(bstringToBuffer(reader.result));
        };
        reader.readAsBinaryString(this.blob);
    }

} else {
    // if (console && console.log)
    //    console.log('defining sync BlobFetchable.fetch');

    BlobFetchable.prototype.fetch = function(callback) {
        var reader = new FileReaderSync();
        try {
            var res = reader.readAsArrayBuffer(this.blob);
            callback(res);
        } catch (e) {
            callback(null, e);
        }
    }
}

function URLFetchable(url, start, end, opts) {
    if (!opts) {
        if (typeof start === 'object') {
            opts = start;
            start = undefined;
        } else {
            opts = {};
        }
    }

    this.url = url;
    this.start = start || 0;
    if (end) {
        this.end = end;
    }
    this.opts = opts;
}

URLFetchable.prototype.slice = function(s, l) {
    if (s < 0) {
        throw 'Bad slice ' + s;
    }

    var ns = this.start, ne = this.end;
    if (ns && s) {
        ns = ns + s;
    } else {
        ns = s || ns;
    }
    if (l && ns) {
        ne = ns + l - 1;
    } else {
        ne = ne || l - 1;
    }
    return new URLFetchable(this.url, ns, ne, this.opts);
}

var seed=0;
var isSafari = navigator.userAgent.indexOf('Safari') >= 0 && navigator.userAgent.indexOf('Chrome') < 0 ;

URLFetchable.prototype.fetchAsText = function(callback) {
    var req = new XMLHttpRequest();
    var length;
    var url = this.url;
    if (isSafari || this.opts.salt) {
        url = url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++seed));
    }
    req.open('GET', url, true);

    if (this.end) {
        if (this.end - this.start > 100000000) {
            throw 'Monster fetch!';
        }
        req.setRequestHeader('Range', 'bytes=' + this.start + '-' + this.end);
        length = this.end - this.start + 1;
    }

    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if (req.status == 200 || req.status == 206) {
                return callback(req.responseText);
            } else {
                return callback(null);
            }
        }
    };
    if (this.opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

URLFetchable.prototype.salted = function() {
    var o = shallowCopy(this.opts);
    o.salt = true;
    return new URLFetchable(this.url, this.start, this.end, o);
}

URLFetchable.prototype.fetch = function(callback, attempt, truncatedLength) {
    var thisB = this;

    attempt = attempt || 1;
    if (attempt > 3) {
        return callback(null);
    }

    var req = new XMLHttpRequest();
    var length;
    var url = this.url;
    if (isSafari || this.opts.salt) {
        url = url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++seed));
    }
    req.open('GET', url, true);
    req.overrideMimeType('text/plain; charset=x-user-defined');
    if (this.end) {
        if (this.end - this.start > 100000000) {
            throw 'Monster fetch!';
        }
        req.setRequestHeader('Range', 'bytes=' + this.start + '-' + this.end);
        length = this.end - this.start + 1;
    }
    req.responseType = 'arraybuffer';
    req.onreadystatechange = function() {
        if (req.readyState == 4) {
            if (req.status == 200 || req.status == 206) {
                if (req.response) {
                    var bl = req.response.byteLength;
                    if (length && length != bl && (!truncatedLength || bl != truncatedLength)) {
                        return thisB.fetch(callback, attempt + 1, bl);
                    } else {
                        return callback(req.response);
                    }
                } else if (req.mozResponseArrayBuffer) {
                    return callback(req.mozResponseArrayBuffer);
                } else {
                    var r = req.responseText;
                    if (length && length != r.length && (!truncatedLength || r.length != truncatedLength)) {
                        return thisB.fetch(callback, attempt + 1, r.length);
                    } else {
                        return callback(bstringToBuffer(req.responseText));
                    }
                }
            } else {
                return thisB.fetch(callback, attempt + 1);
            }
        }
    };
    if (this.opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

function bstringToBuffer(result) {
    if (!result) {
        return null;
    }

    var ba = new Uint8Array(result.length);
    for (var i = 0; i < ba.length; ++i) {
        ba[i] = result.charCodeAt(i);
    }
    return ba.buffer;
}

// Read from Uint8Array

(function(global) {
    var convertBuffer = new ArrayBuffer(8);
    var ba = new Uint8Array(convertBuffer);
    var fa = new Float32Array(convertBuffer);


    global.readFloat = function(buf, offset) {
        ba[0] = buf[offset];
        ba[1] = buf[offset+1];
        ba[2] = buf[offset+2];
        ba[3] = buf[offset+3];
        return fa[0];
    };
 }(this));

function readInt64(ba, offset) {
    return (ba[offset + 7] << 24) | (ba[offset + 6] << 16) | (ba[offset + 5] << 8) | (ba[offset + 4]);
}

function readInt(ba, offset) {
    return (ba[offset + 3] << 24) | (ba[offset + 2] << 16) | (ba[offset + 1] << 8) | (ba[offset]);
}

function readShort(ba, offset) {
    return (ba[offset + 1] << 8) | (ba[offset]);
}

function readByte(ba, offset) {
    return ba[offset];
}

function readIntBE(ba, offset) {
    return (ba[offset] << 24) | (ba[offset + 1] << 16) | (ba[offset + 2] << 8) | (ba[offset + 3]);
}

// Exports if we are being used as a module

if (typeof(module) !== 'undefined') {
    module.exports = {
        BlobFetchable: BlobFetchable,
        URLFetchable: URLFetchable,

        readInt: readInt,
        readIntBE: readIntBE,
        readInt64: readInt64,
        readShort: readShort,
        readByte: readByte,
        readFloat: this.readFloat
    }
}

},{"./sha1":8,"./utils":10}],4:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// color.js
//

"use strict";

function DColour(red, green, blue, name) {
    this.red = red|0;
    this.green = green|0;
    this.blue = blue|0;
    if (name) {
        this.name = name;
    }
}

DColour.prototype.toSvgString = function() {
    if (!this.name) {
        this.name = "rgb(" + this.red + "," + this.green + "," + this.blue + ")";
    }

    return this.name;
}

function hex2(x) {
    var y = '00' + x.toString(16);
    return y.substring(y.length - 2);
}

DColour.prototype.toHexString = function() {
    return '#' + hex2(this.red) + hex2(this.green) + hex2(this.blue);
}

var palette = {
    red: new DColour(255, 0, 0, 'red'),
    green: new DColour(0, 255, 0, 'green'),
    blue: new DColour(0, 0, 255, 'blue'),
    yellow: new DColour(255, 255, 0, 'yellow'),
    white: new DColour(255, 255, 255, 'white'),
    black: new DColour(0, 0, 0, 'black'),
    gray: new DColour(180, 180, 180, 'gray'),
    grey: new DColour(180, 180, 180, 'grey'),
    lightskyblue: new DColour(135, 206, 250, 'lightskyblue'),
    lightsalmon: new DColour(255, 160, 122, 'lightsalmon'),
    hotpink: new DColour(255, 105, 180, 'hotpink')
};

var COLOR_RE = new RegExp('^#([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})$');
var CSS_COLOR_RE = /rgb\(([0-9]+),([0-9]+),([0-9]+)\)/

function dasColourForName(name) {
    var c = palette[name];
    if (!c) {
        var match = COLOR_RE.exec(name);
        if (match) {
            c = new DColour(('0x' + match[1])|0, ('0x' + match[2])|0, ('0x' + match[3])|0, name);
            palette[name] = c;
        } else {
    	    match = CSS_COLOR_RE.exec(name);
    	    if (match) {
        		c = new DColour(match[1]|0, match[2]|0, match[3]|0, name);
        		palette[name] = c;
	       } else {
		      console.log("couldn't handle color: " + name);
		      c = palette.black;
		      palette[name] = c;
	       }
        }
    }
    return c;
}

function makeColourSteps(steps, stops, colours) {
    var dcolours = [];
    for (var ci = 0; ci < colours.length; ++ci) {
        dcolours.push(dasColourForName(colours[ci]));
    }

    var grad = [];
  STEP_LOOP:
    for (var si = 0; si < steps; ++si) {
        var rs = (1.0 * si) / (steps-1);
        var score = stops[0] + (stops[stops.length -1] - stops[0]) * rs;
        for (var i = 0; i < stops.length - 1; ++i) {
            if (score >= stops[i] && score <= stops[i+1]) {
                var frac = (score - stops[i]) / (stops[i+1] - stops[i]);
                var ca = dcolours[i];
                var cb = dcolours[i+1];

                var fill = new DColour(
                    ((ca.red * (1.0 - frac)) + (cb.red * frac))|0,
                    ((ca.green * (1.0 - frac)) + (cb.green * frac))|0,
                    ((ca.blue * (1.0 - frac)) + (cb.blue * frac))|0
                ).toSvgString();
                grad.push(fill);

                continue STEP_LOOP;
            }
        }
        throw 'Bad step';
    }

    return grad;
}

function makeGradient(steps, color1, color2, color3) {
    if (color3) {
        return makeColourSteps(steps, [0, 0.5, 1], [color1, color2, color3]);
    } else {
        return makeColourSteps(steps, [0, 1], [color1, color2]);
    }
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        makeColourSteps: makeColourSteps,
        makeGradient: makeGradient,
        dasColourForName: dasColourForName
    };
}

},{}],5:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// das.js: queries and low-level data model.
//

"use strict";

if (typeof(require) !== 'undefined') {
    var utils = require('./utils');
    var shallowCopy = utils.shallowCopy;
    var pusho = utils.pusho;

    var color = require('./color');
    var makeColourSteps = color.makeColourSteps;
}

var dasLibErrorHandler = function(errMsg) {
    alert(errMsg);
}
var dasLibRequestQueue = new Array();



function DASSegment(name, start, end, description) {
    this.name = name;
    this.start = start;
    this.end = end;
    this.description = description;
}
DASSegment.prototype.toString = function() {
    return this.name + ':' + this.start + '..' + this.end;
};
DASSegment.prototype.isBounded = function() {
    return this.start && this.end;
}
DASSegment.prototype.toDASQuery = function() {
    var q = 'segment=' + this.name;
    if (this.start && this.end) {
        q += (':' + this.start + ',' + this.end);
    }
    return q;
}


function DASSource(a1, a2) {
    var options;
    if (typeof a1 == 'string') {
        this.uri = a1;
        options = a2 || {};
    } else {
        options = a1 || {};
    }
    for (var k in options) {
        if (typeof(options[k]) != 'function') {
            this[k] = options[k];
        }
    }


    if (!this.coords) {
        this.coords = [];
    }
    if (!this.props) {
        this.props = {};
    }

    this.dasBaseURI = this.uri;
    if (this.dasBaseURI && this.dasBaseURI.substr(this.uri.length - 1) != '/') {
        this.dasBaseURI = this.dasBaseURI + '/';
    }
}

function DASCoords() {
}

function coordsMatch(c1, c2) {
    return c1.taxon == c2.taxon && c1.auth == c2.auth && c1.version == c2.version;
}

//
// DAS 1.6 entry_points command
//

DASSource.prototype.entryPoints = function(callback) {
    var dasURI = this.dasBaseURI + 'entry_points';
    this.doCrossDomainRequest(dasURI, function(responseXML) {
            if (!responseXML) {
                return callback([]);
            }

                var entryPoints = new Array();
                
                var segs = responseXML.getElementsByTagName('SEGMENT');
                for (var i = 0; i < segs.length; ++i) {
                    var seg = segs[i];
                    var segId = seg.getAttribute('id');
                    
                    var segSize = seg.getAttribute('size');
                    var segMin, segMax;
                    if (segSize) {
                        segMin = 1; segMax = segSize|0;
                    } else {
                        segMin = seg.getAttribute('start');
                        if (segMin) {
                            segMin |= 0;
                        }
                        segMax = seg.getAttribute('stop');
                        if (segMax) {
                            segMax |= 0;
                        }
                    }
                    var segDesc = null;
                    if (seg.firstChild) {
                        segDesc = seg.firstChild.nodeValue;
                    }
                    entryPoints.push(new DASSegment(segId, segMin, segMax, segDesc));
                }          
               callback(entryPoints);
    });         
}

//
// DAS 1.6 sequence command
// Do we need an option to fall back to the dna command?
//

function DASSequence(name, start, end, alpha, seq) {
    this.name = name;
    this.start = start;
    this.end = end;
    this.alphabet = alpha;
    this.seq = seq;
}

DASSource.prototype.sequence = function(segment, callback) {
    var dasURI = this.dasBaseURI + 'sequence?' + segment.toDASQuery();
    this.doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            callback([]);
            return;
        } else {
                var seqs = new Array();
                
                var segs = responseXML.getElementsByTagName('SEQUENCE');
                for (var i = 0; i < segs.length; ++i) {
                    var seg = segs[i];
                    var segId = seg.getAttribute('id');
                    var segMin = seg.getAttribute('start');
                    var segMax = seg.getAttribute('stop');
                    var segAlpha = 'DNA';
                    var segSeq = null;
                    if (seg.firstChild) {
                        var rawSeq = seg.firstChild.nodeValue;
                        segSeq = '';
                        var idx = 0;
                        while (true) {
                            var space = rawSeq.indexOf('\n', idx);
                            if (space >= 0) {
                                segSeq += rawSeq.substring(idx, space).toUpperCase();
                                idx = space + 1;
                            } else {
                                segSeq += rawSeq.substring(idx).toUpperCase();
                                break;
                            }
                        }
                    }
                    seqs.push(new DASSequence(segId, segMin, segMax, segAlpha, segSeq));
                }
                
                callback(seqs);
        }
    });
}

//
// DAS 1.6 features command
//

function DASFeature() {
}

function DASGroup(id) {
    if (id)
        this.id = id;
}

function DASLink(desc, uri) {
    this.desc = desc;
    this.uri = uri;
}

DASSource.prototype.features = function(segment, options, callback) {
    options = options || {};
    var thisB = this;

    var dasURI;
    if (this.features_uri) {
        dasURI = this.features_uri;
    } else {
        var filters = [];

        if (segment) {
            filters.push(segment.toDASQuery());
        } else if (options.group) {
            var g = options.group;
            if (typeof g == 'string') {
                filters.push('group_id=' + g);
            } else {
                for (var gi = 0; gi < g.length; ++gi) {
                    filters.push('group_id=' + g[gi]);
                }
            }
        }

        if (options.adjacent) {
            var adj = options.adjacent;
            if (typeof adj == 'string') {
                adj = [adj];
            }
            for (var ai = 0; ai < adj.length; ++ai) {
                filters.push('adjacent=' + adj[ai]);
            }
        }

        if (options.type) {
            if (typeof options.type == 'string') {
                filters.push('type=' + options.type);
            } else {
                for (var ti = 0; ti < options.type.length; ++ti) {
                    filters.push('type=' + options.type[ti]);
                }
            }
        }
        
        if (options.maxbins) {
            filters.push('maxbins=' + options.maxbins);
        }
        
        if (filters.length > 0) {
            dasURI = this.dasBaseURI + 'features?' + filters.join(';');
        } else {
            callback([], 'No filters specified');
        }
    } 
   

    this.doCrossDomainRequest(dasURI, function(responseXML, req) {
        if (!responseXML) {
            var msg;
            if (req.status == 0) {
                msg = 'server may not support CORS';
            } else {
                msg = 'status=' + req.status;
            }
            callback([], 'Failed request: ' + msg);
            return;
        }
/*      if (req) {
            var caps = req.getResponseHeader('X-DAS-Capabilties');
            if (caps) {
                alert(caps);
            }
        } */

        var features = new Array();
        var segmentMap = {};

        var segs = responseXML.getElementsByTagName('SEGMENT');
        for (var si = 0; si < segs.length; ++si) {
            var segmentXML = segs[si];
            var segmentID = segmentXML.getAttribute('id');
            segmentMap[segmentID] = {
                min: segmentXML.getAttribute('start'),
                max: segmentXML.getAttribute('stop')
            };
            
            var featureXMLs = segmentXML.getElementsByTagName('FEATURE');
            for (var i = 0; i < featureXMLs.length; ++i) {
                var feature = featureXMLs[i];
                var dasFeature = new DASFeature();
                
                dasFeature.segment = segmentID;
                dasFeature.id = feature.getAttribute('id');
                dasFeature.label = feature.getAttribute('label');


/*
                var childNodes = feature.childNodes;
                for (var c = 0; c < childNodes.length; ++c) {
                    var cn = childNodes[c];
                    if (cn.nodeType == Node.ELEMENT_NODE) {
                        var key = cn.tagName;
                        //var val = null;
                        //if (cn.firstChild) {
                        //   val = cn.firstChild.nodeValue;
                        //}
                        dasFeature[key] = 'x';
                    }
                } */


                var spos = elementValue(feature, "START");
                var epos = elementValue(feature, "END");
                if ((spos|0) > (epos|0)) {
                    dasFeature.min = epos|0;
                    dasFeature.max = spos|0;
                } else {
                    dasFeature.min = spos|0;
                    dasFeature.max = epos|0;
                }
                {
                    var tec = feature.getElementsByTagName('TYPE');
                    if (tec.length > 0) {
                        var te = tec[0];
                        if (te.firstChild) {
                            dasFeature.type = te.firstChild.nodeValue;
                        }
                        dasFeature.typeId = te.getAttribute('id');
                        dasFeature.typeCv = te.getAttribute('cvId');
                    }
                }
                dasFeature.type = elementValue(feature, "TYPE");
                if (!dasFeature.type && dasFeature.typeId) {
                    dasFeature.type = dasFeature.typeId; // FIXME?
                }
                
                dasFeature.method = elementValue(feature, "METHOD");
                {
                    var ori = elementValue(feature, "ORIENTATION");
                    if (!ori) {
                        ori = '0';
                    }
                    dasFeature.orientation = ori;
                }
                dasFeature.score = elementValue(feature, "SCORE");
                dasFeature.links = dasLinksOf(feature);
                dasFeature.notes = dasNotesOf(feature);
                
                var groups = feature.getElementsByTagName("GROUP");
                for (var gi  = 0; gi < groups.length; ++gi) {
                    var groupXML = groups[gi];
                    var dasGroup = new DASGroup();
                    dasGroup.type = groupXML.getAttribute('type');
                    dasGroup.id = groupXML.getAttribute('id');
                    dasGroup.links = dasLinksOf(groupXML);
                    dasGroup.notes = dasNotesOf(groupXML);
                    if (!dasFeature.groups) {
                        dasFeature.groups = new Array(dasGroup);
                    } else {
                        dasFeature.groups.push(dasGroup);
                    }
                }

                // Magic notes.  Check with TAD before changing this.
                if (dasFeature.notes) {
                    for (var ni = 0; ni < dasFeature.notes.length; ++ni) {
                        var n = dasFeature.notes[ni];
                        if (n.indexOf('Genename=') == 0) {
                            var gg = new DASGroup();
                            gg.type='gene';
                            gg.id = n.substring(9);
                            if (!dasFeature.groups) {
                                dasFeature.groups = new Array(gg);
                            } else {
                                dasFeature.groups.push(gg);
                            }
                        }
                    }
                }
                
                {
                    var pec = feature.getElementsByTagName('PART');
                    if (pec.length > 0) {
                        var parts = [];
                        for (var pi = 0; pi < pec.length; ++pi) {
                            parts.push(pec[pi].getAttribute('id'));
                        }
                        dasFeature.parts = parts;
                    }
                }
                {
                    var pec = feature.getElementsByTagName('PARENT');
                    if (pec.length > 0) {
                        var parents = [];
                        for (var pi = 0; pi < pec.length; ++pi) {
                            parents.push(pec[pi].getAttribute('id'));
                        }
                        dasFeature.parents = parents;
                    }
                }
                
                features.push(dasFeature);
            }
        }
                
        callback(features, undefined, segmentMap);
    },
    function (err) {
        callback([], err);
    });
}

function DASAlignment(type) {
    this.type = type;
    this.objects = {};
    this.blocks = [];
}

DASSource.prototype.alignments = function(segment, options, callback) {
    var dasURI = this.dasBaseURI + 'alignment?query=' + segment;
    this.doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            callback([], 'Failed request ' + dasURI);
            return;
        }

        var alignments = [];
        var aliXMLs = responseXML.getElementsByTagName('alignment');
        for (var ai = 0; ai < aliXMLs.length; ++ai) {
            var aliXML = aliXMLs[ai];
            var ali = new DASAlignment(aliXML.getAttribute('alignType'));
            var objXMLs = aliXML.getElementsByTagName('alignObject');
            for (var oi = 0; oi < objXMLs.length; ++oi) {
                var objXML = objXMLs[oi];
                var obj = {
                    id:          objXML.getAttribute('intObjectId'),
                    accession:   objXML.getAttribute('dbAccessionId'),
                    version:     objXML.getAttribute('objectVersion'),
                    dbSource:    objXML.getAttribute('dbSource'),
                    dbVersion:   objXML.getAttribute('dbVersion')
                };
                ali.objects[obj.id] = obj;
            }
            
            var blockXMLs = aliXML.getElementsByTagName('block');
            for (var bi = 0; bi < blockXMLs.length; ++bi) {
                var blockXML = blockXMLs[bi];
                var block = {
                    order:      blockXML.getAttribute('blockOrder'),
                    segments:   []
                };
                var segXMLs = blockXML.getElementsByTagName('segment');
                for (var si = 0; si < segXMLs.length; ++si) {
                    var segXML = segXMLs[si];
                    var seg = {
                        object:      segXML.getAttribute('intObjectId'),
                        min:         segXML.getAttribute('start'),
                        max:         segXML.getAttribute('end'),
                        strand:      segXML.getAttribute('strand'),
                        cigar:       elementValue(segXML, 'cigar')
                    };
                    block.segments.push(seg);
                }
                ali.blocks.push(block);
            }       
                    
            alignments.push(ali);
        }
        callback(alignments);
    });
}


function DASStylesheet() {
/*
    this.highZoomStyles = new Object();
    this.mediumZoomStyles = new Object();
    this.lowZoomStyles = new Object();
*/

    this.styles = [];
}

DASStylesheet.prototype.pushStyle = function(filters, zoom, style) {
    /*

    if (!zoom) {
        this.highZoomStyles[type] = style;
        this.mediumZoomStyles[type] = style;
        this.lowZoomStyles[type] = style;
    } else if (zoom == 'high') {
        this.highZoomStyles[type] = style;
    } else if (zoom == 'medium') {
        this.mediumZoomStyles[type] = style;
    } else if (zoom == 'low') {
        this.lowZoomStyles[type] = style;
    }

    */

    if (!filters) {
        filters = {type: 'default'};
    }
    var styleHolder = shallowCopy(filters);
    if (zoom) {
        styleHolder.zoom = zoom;
    }
    styleHolder.style = style;
    this.styles.push(styleHolder);
}

function DASStyle() {
}

function parseGradient(grad) {
    var steps = grad.getAttribute('steps');
    if (steps) {
        steps = steps|0;
    } else {
        steps = 50;
    }


    var stops = [];
    var colors = [];
    var se = grad.getElementsByTagName('STOP');
    for (var si = 0; si < se.length; ++si) {
        var stop = se[si];
        stops.push(1.0 * stop.getAttribute('score'));
        colors.push(stop.firstChild.nodeValue);
    }

    return makeColourSteps(steps, stops, colors);
}

DASSource.prototype.stylesheet = function(successCB, failureCB) {
    var dasURI, creds = this.credentials;
    if (this.stylesheet_uri) {
        dasURI = this.stylesheet_uri;
        creds = false;
    } else {
        dasURI = this.dasBaseURI + 'stylesheet';
    }

    doCrossDomainRequest(dasURI, function(responseXML) {
        if (!responseXML) {
            if (failureCB) {
                failureCB();
            } 
            return;
        }
        var stylesheet = new DASStylesheet();
        var typeXMLs = responseXML.getElementsByTagName('TYPE');
        for (var i = 0; i < typeXMLs.length; ++i) {
            var typeStyle = typeXMLs[i];
            
            var filter = {};
            filter.type = typeStyle.getAttribute('id'); // Am I right in thinking that this makes DASSTYLE XML invalid?  Ugh.
            filter.label = typeStyle.getAttribute('label');
            filter.method = typeStyle.getAttribute('method');
            var glyphXMLs = typeStyle.getElementsByTagName('GLYPH');
            for (var gi = 0; gi < glyphXMLs.length; ++gi) {
                var glyphXML = glyphXMLs[gi];
                var zoom = glyphXML.getAttribute('zoom');
                var glyph = childElementOf(glyphXML);
                var style = new DASStyle();
                style.glyph = glyph.localName;
                var child = glyph.firstChild;
        
                while (child) {
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        // alert(child.localName);
                        if (child.localName == 'BGGRAD') {
                            style[child.localName] = parseGradient(child);
                        } else {      
                            style[child.localName] = child.firstChild.nodeValue;
                        }
                    }
                    child = child.nextSibling;
                }
                stylesheet.pushStyle(filter, zoom, style);
            }
        }
        successCB(stylesheet);
    }, creds);
}

//
// sources command
// 

function DASRegistry(uri, opts)
{
    opts = opts || {};
    this.uri = uri;
    this.opts = opts;   
}

DASRegistry.prototype.sources = function(callback, failure, opts)
{
    if (!opts) {
        opts = {};
    }

    var filters = [];
    if (opts.taxon) {
        filters.push('organism=' + opts.taxon);
    }
    if (opts.auth) {
        filters.push('authority=' + opts.auth);
    }
    if (opts.version) {
        filters.push('version=' + opts.version);
    }
    var quri = this.uri;
    if (filters.length > 0) {
        quri = quri + '?' + filters.join('&');   // '&' as a separator to hack around dasregistry.org bug.
    }

    doCrossDomainRequest(quri, function(responseXML) {
        if (!responseXML && failure) {
            failure();
            return;
        }

        var sources = [];       
        var sourceXMLs = responseXML.getElementsByTagName('SOURCE');
        for (var si = 0; si < sourceXMLs.length; ++si) {
            var sourceXML = sourceXMLs[si];
            var versionXMLs = sourceXML.getElementsByTagName('VERSION');
            if (versionXMLs.length < 1) {
                continue;
            }
            var versionXML = versionXMLs[0];

            var coordXMLs = versionXML.getElementsByTagName('COORDINATES');
            var coords = [];
            for (var ci = 0; ci < coordXMLs.length; ++ci) {
                var coordXML = coordXMLs[ci];
                var coord = new DASCoords();
                coord.auth = coordXML.getAttribute('authority');
                coord.taxon = coordXML.getAttribute('taxid');
                coord.version = coordXML.getAttribute('version');
                coords.push(coord);
            }
            
            var caps = [];
            var capXMLs = versionXML.getElementsByTagName('CAPABILITY');
            var uri;
            for (var ci = 0; ci < capXMLs.length; ++ci) {
                var capXML = capXMLs[ci];
                
                caps.push(capXML.getAttribute('type'));

                if (capXML.getAttribute('type') == 'das1:features') {
                    var fep = capXML.getAttribute('query_uri');
                    uri = fep.substring(0, fep.length - ('features'.length));
                }
            }

            var props = {};
            var propXMLs = versionXML.getElementsByTagName('PROP');
            for (var pi = 0; pi < propXMLs.length; ++pi) {
                pusho(props, propXMLs[pi].getAttribute('name'), propXMLs[pi].getAttribute('value'));
            }
            
            if (uri) {
                var source = new DASSource(uri, {
                    source_uri: sourceXML.getAttribute('uri'),
                    name:  sourceXML.getAttribute('title'),
                    desc:  sourceXML.getAttribute('description'),
                    coords: coords,
                    props: props,
                    capabilities: caps
                });
                sources.push(source);
            }
        }
        
        callback(sources);
    });
}


//
// Utility functions
//

function elementValue(element, tag)
{
    var children = element.getElementsByTagName(tag);
    if (children.length > 0 && children[0].firstChild) {
        var c = children[0];
        if (c.childNodes.length == 1) {
            return c.firstChild.nodeValue;
        } else {
            var s = '';
            for (var ni = 0; ni < c.childNodes.length; ++ni) {
                s += c.childNodes[ni].nodeValue;
            }
            return s;
        }

    } else {
        return null;
    }
}

function childElementOf(element)
{
    if (element.hasChildNodes()) {
        var child = element.firstChild;
        do {
            if (child.nodeType == Node.ELEMENT_NODE) {
                return child;
            } 
            child = child.nextSibling;
        } while (child != null);
    }
    return null;
}


function dasLinksOf(element)
{
    var links = new Array();
    var maybeLinkChilden = element.getElementsByTagName('LINK');
    for (var ci = 0; ci < maybeLinkChilden.length; ++ci) {
        var linkXML = maybeLinkChilden[ci];
        if (linkXML.parentNode == element) {
            links.push(new DASLink(linkXML.firstChild ? linkXML.firstChild.nodeValue : 'Unknown', linkXML.getAttribute('href')));
        }
    }
    
    return links;
}

function dasNotesOf(element)
{
    var notes = [];
    var maybeNotes = element.getElementsByTagName('NOTE');
    for (var ni = 0; ni < maybeNotes.length; ++ni) {
        if (maybeNotes[ni].firstChild) {
            notes.push(maybeNotes[ni].firstChild.nodeValue);
        }
    }
    return notes;
}

function doCrossDomainRequest(url, handler, credentials, custAuth) {
    // TODO: explicit error handlers?

    if (window.XDomainRequest) {
        var req = new XDomainRequest();
        req.onload = function() {
            var dom = new ActiveXObject("Microsoft.XMLDOM");
            dom.async = false;
            dom.loadXML(req.responseText);
            handler(dom);
        }
        req.open("get", url);
        req.send('');
    } else {
        var reqStart = Date.now();
        var req = new XMLHttpRequest();

        req.onreadystatechange = function() {
            if (req.readyState == 4) {
              if (req.status >= 200 || req.status == 0) {
                  handler(req.responseXML, req);
              }
            }
        };
        req.open("get", url, true);
        if (credentials) {
            req.withCredentials = true;
        }
        if (custAuth) {
            req.setRequestHeader('X-DAS-Authorisation', custAuth);
        }
        req.overrideMimeType('text/xml');
        req.setRequestHeader('Accept', 'application/xml,*/*');
        req.send('');
    }
}

DASSource.prototype.doCrossDomainRequest = function(url, handler, errHandler) {
    var custAuth;
    if (this.xUser) {
        custAuth = 'Basic ' + btoa(this.xUser + ':' + this.xPass);
    }

    try {
        return doCrossDomainRequest(url, handler, this.credentials, custAuth);
    } catch (err) {
        if (errHandler) {
            errHandler(err);
        } else {
            throw err;
        }
    }
}

function isDasBooleanTrue(s) {
    s = ('' + s).toLowerCase();
    return s==='yes' || s==='true';
}

function isDasBooleanNotFalse(s) {
    if (!s)
        return false;

    s = ('' + s).toLowerCase();
    return s!=='no' || s!=='false';
}

function copyStylesheet(ss) {
    var nss = shallowCopy(ss);
    nss.styles = [];
    for (var si = 0; si < ss.styles.length; ++si) {
        var sh = nss.styles[si] = shallowCopy(ss.styles[si]);
        sh._methodRE = sh._labelRE = sh._typeRE = undefined;
        sh.style = shallowCopy(sh.style);
        sh.style.id = undefined;
        sh.style._gradient = undefined;
    }
    return nss;
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        DASGroup: DASGroup,
        DASFeature: DASFeature,
        DASStylesheet: DASStylesheet,
        DASStyle: DASStyle,
        DASSource: DASSource,
        DASSegment: DASSegment,
        DASRegistry: DASRegistry,
        DASSequence: DASSequence,
        DASLink: DASLink,

        isDasBooleanTrue: isDasBooleanTrue,
        isDasBooleanNotFalse: isDasBooleanNotFalse,
        copyStylesheet: copyStylesheet,
        coordsMatch: coordsMatch
    };
}
},{"./color":4,"./utils":10}],6:[function(require,module,exports){
(function (global){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2014
//
// fetchworker.js
//

"use strict";

var bin = require('./bin');
var bam = require('./bam');
var bigwig = require('./bigwig');

var connections = {};

var idSeed = 0;

global.newID = function() {
    return 'cn' + (++idSeed);
}

postMessage({tag: 'init'});

self.onmessage = function(event) {
    var d = event.data;
    var command = event.data.command;
    var tag = event.data.tag;

    if (command === 'connectBAM') {
        var id = newID();

        var bamF, baiF, indexChunks;
        if (d.blob) {
            bamF = new bin.BlobFetchable(d.blob);
            baiF = new bin.BlobFetchable(d.indexBlob);
        } else {
            bamF = new bin.URLFetchable(d.uri, {credentials: d.credentials});
            baiF = new bin.URLFetchable(d.indexUri, {credentials: d.credentials});
            indexChunks = d.indexChunks;
        }

        bam.makeBam(bamF, baiF, indexChunks, function(bamObj, err) {
            if (bamObj) {
                connections[id] = new BAMWorkerFetcher(bamObj);
                postMessage({tag: tag, result: id});
            } else {
                postMessage({tag: tag, error: err || "Couldn't fetch BAM"});
            }
        });
    } else if (command === 'connectBBI') {
        var id = newID();
        var bbi;
        if (d.blob) {
            bbi = new bin.BlobFetchable(d.blob);
        } else {
            bbi = new bin.URLFetchable(d.uri, {credentials: d.credentials});
        }

        bigwig.makeBwg(bbi, function(bwg, err) {
            if (bwg) {
                connections[id] = new BBIWorkerFetcher(bwg);
                postMessage({tag: tag, result: id});
            } else {
                postMessage({tag: tag, error: err || "Couldn't fetch BBI"});
            }
        }, d.uri);
    } else if (command === 'fetch') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.fetch(d.tag, d.chr, d.min, d.max, d.zoom, d.opts);
    } else if (command === 'leap') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.leap(d.tag, d.chr, d.pos, d.dir);
    } else if (command === 'quantLeap') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.quantLeap(d.tag, d.chr, d.pos, d.dir, d.threshold, d.under);
    } else if (command === 'meta') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.meta(d.tag);
    } else if (command === 'search') {
        var con = connections[event.data.connection];
        if (!con) {
            return postMessage({tag: tag, error: 'No such connection: ' + event.data.connection});
        }

        con.search(d.tag, d.query, d.index);
    } else if (command === 'date') {
        return postMessage({tag: tag, result: Date.now()|0});
    } else {
        postMessage({tag: tag, error: 'Bad command ' + command});
    }
}

function BAMWorkerFetcher(bam) {
    this.bam = bam;
}

BAMWorkerFetcher.prototype.fetch = function(tag, chr, min, max, zoom, opts) {
    opts = opts || {};
    this.bam.fetch(chr, min, max, function(records, err) {
        if (records) {
            postMessage({tag: tag, result: records, time: Date.now()|0});
        } else {
            postMessage({tag: tag, error: err});
        }
    }, opts);
}

function BBIWorkerFetcher(bbi) {
    this.bbi = bbi;
}

BBIWorkerFetcher.prototype.fetch = function(tag, chr, min, max, zoom) {
    if (typeof(zoom) !== 'number')
        zoom = -1;

    var data;
    if (zoom < 0) {
        data = this.bbi.getUnzoomedView();
    } else {
        data = this.bbi.getZoomedView(zoom);
    }

    data.readWigData(chr, min, max, function(features) {
        postMessage({tag: tag, result: features});
    });
}

BBIWorkerFetcher.prototype.meta = function(tag) {
    var scales = [1];
    for (var z = 0; z < this.bbi.zoomLevels.length; ++z) {
        scales.push(this.bbi.zoomLevels[z].reduction);
    }

    var thisB = this;
    var meta = {type: this.bbi.type,
                zoomLevels: scales,
                fieldCount: this.bbi.fieldCount,
                definedFieldCount: this.bbi.definedFieldCount,
                schema: this.bbi.schema};
    if (this.bbi.type === 'bigbed') {
        this.bbi.getExtraIndices(function(ei) {
            if (ei) {
                thisB.extraIndices = ei;
                meta.extraIndices = ei.map(function(i) {return i.field});
            }
            postMessage({tag: tag, result: meta});
        });
    } else {
        postMessage({tag: tag, result: meta});
    }
}

BBIWorkerFetcher.prototype.leap = function(tag, chr, pos, dir) {
    this.bbi.getUnzoomedView().getFirstAdjacent(chr, pos, dir, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

BBIWorkerFetcher.prototype.quantLeap = function(tag, chr, pos, dir, threshold, under) {
    this.bbi.thresholdSearch(chr, pos, dir, threshold, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

BBIWorkerFetcher.prototype.search = function(tag, query, index) {
    var is = this.extraIndices[0];
    is.lookup(query, function(result, err) {
        postMessage({tag: tag, result: result, error: err});
    });
}

}).call(this,typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {})
},{"./bam":1,"./bigwig":2,"./bin":3}],7:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2011
//
// lh3utils.js: common support for lh3's file formats
//

if (typeof(require) !== 'undefined') {
    var jszlib = require('jszlib');
    var jszlib_inflate_buffer = jszlib.inflateBuffer;
    var arrayCopy = jszlib.arrayCopy;
}

function Vob(b, o) {
    this.block = b;
    this.offset = o;
}

Vob.prototype.toString = function() {
    return '' + this.block + ':' + this.offset;
}

function readVob(ba, offset) {
    var block = ((ba[offset+6] & 0xff) * 0x100000000) + ((ba[offset+5] & 0xff) * 0x1000000) + ((ba[offset+4] & 0xff) * 0x10000) + ((ba[offset+3] & 0xff) * 0x100) + ((ba[offset+2] & 0xff));
    var bint = (ba[offset+1] << 8) | (ba[offset]);
    if (block == 0 && bint == 0) {
        return null;  // Should only happen in the linear index?
    } else {
        return new Vob(block, bint);
    }
}

function unbgzf(data, lim) {
    lim = Math.min(lim || 1, data.byteLength - 50);
    var oBlockList = [];
    var ptr = [0];
    var totalSize = 0;

    while (ptr[0] < lim) {
        var ba = new Uint8Array(data, ptr[0], 12); // FIXME is this enough for all credible BGZF block headers?
        var xlen = (ba[11] << 8) | (ba[10]);
        // dlog('xlen[' + (ptr[0]) +']=' + xlen);
        var unc = jszlib_inflate_buffer(data, 12 + xlen + ptr[0], Math.min(65536, data.byteLength - 12 - xlen - ptr[0]), ptr);
        ptr[0] += 8;
        totalSize += unc.byteLength;
        oBlockList.push(unc);
    }

    if (oBlockList.length == 1) {
        return oBlockList[0];
    } else {
        var out = new Uint8Array(totalSize);
        var cursor = 0;
        for (var i = 0; i < oBlockList.length; ++i) {
            var b = new Uint8Array(oBlockList[i]);
            arrayCopy(b, 0, out, cursor, b.length);
            cursor += b.length;
        }
        return out.buffer;
    }
}

function Chunk(minv, maxv) {
    this.minv = minv; this.maxv = maxv;
}


//
// Binning (transliterated from SAM1.3 spec)
//

/* calculate bin given an alignment covering [beg,end) (zero-based, half-close-half-open) */
function reg2bin(beg, end)
{
    --end;
    if (beg>>14 == end>>14) return ((1<<15)-1)/7 + (beg>>14);
    if (beg>>17 == end>>17) return ((1<<12)-1)/7 + (beg>>17);
    if (beg>>20 == end>>20) return ((1<<9)-1)/7 + (beg>>20);
    if (beg>>23 == end>>23) return ((1<<6)-1)/7 + (beg>>23);
    if (beg>>26 == end>>26) return ((1<<3)-1)/7 + (beg>>26);
    return 0;
}

/* calculate the list of bins that may overlap with region [beg,end) (zero-based) */
var MAX_BIN = (((1<<18)-1)/7);
function reg2bins(beg, end) 
{
    var i = 0, k, list = [];
    --end;
    list.push(0);
    for (k = 1 + (beg>>26); k <= 1 + (end>>26); ++k) list.push(k);
    for (k = 9 + (beg>>23); k <= 9 + (end>>23); ++k) list.push(k);
    for (k = 73 + (beg>>20); k <= 73 + (end>>20); ++k) list.push(k);
    for (k = 585 + (beg>>17); k <= 585 + (end>>17); ++k) list.push(k);
    for (k = 4681 + (beg>>14); k <= 4681 + (end>>14); ++k) list.push(k);
    return list;
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        unbgzf: unbgzf,
        readVob: readVob,
        reg2bin: reg2bin,
        reg2bins: reg2bins,
        Chunk: Chunk
    };
}
},{"jszlib":11}],8:[function(require,module,exports){
/*
 * A JavaScript implementation of the Secure Hash Algorithm, SHA-1, as defined
 * in FIPS 180-1
 * Version 2.2 Copyright Paul Johnston 2000 - 2009.
 * Other contributors: Greg Holt, Andrew Kepert, Ydnar, Lostinet
 * Distributed under the BSD License
 * See http://pajhome.org.uk/crypt/md5 for details.
 */

 "use strict";

/*
 * Configurable variables. You may need to tweak these to be compatible with
 * the server-side, but the defaults work in most cases.
 */
var hexcase = 0;  /* hex output format. 0 - lowercase; 1 - uppercase        */
var b64pad  = ""; /* base-64 pad character. "=" for strict RFC compliance   */

/*
 * These are the functions you'll usually want to call
 * They take string arguments and return either hex or base-64 encoded strings
 */
function hex_sha1(s)    { return rstr2hex(rstr_sha1(str2rstr_utf8(s))); }
function b64_sha1(s)    { return rstr2b64(rstr_sha1(str2rstr_utf8(s))); }
function any_sha1(s, e) { return rstr2any(rstr_sha1(str2rstr_utf8(s)), e); }
function hex_hmac_sha1(k, d)
  { return rstr2hex(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d))); }
function b64_hmac_sha1(k, d)
  { return rstr2b64(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d))); }
function any_hmac_sha1(k, d, e)
  { return rstr2any(rstr_hmac_sha1(str2rstr_utf8(k), str2rstr_utf8(d)), e); }

/*
 * Perform a simple self-test to see if the VM is working
 */
function sha1_vm_test()
{
  return hex_sha1("abc").toLowerCase() == "a9993e364706816aba3e25717850c26c9cd0d89d";
}

/*
 * Calculate the SHA1 of a raw string
 */
function rstr_sha1(s)
{
  return binb2rstr(binb_sha1(rstr2binb(s), s.length * 8));
}

/*
 * Calculate the HMAC-SHA1 of a key and some data (raw strings)
 */
function rstr_hmac_sha1(key, data)
{
  var bkey = rstr2binb(key);
  if(bkey.length > 16) bkey = binb_sha1(bkey, key.length * 8);

  var ipad = Array(16), opad = Array(16);
  for(var i = 0; i < 16; i++)
  {
    ipad[i] = bkey[i] ^ 0x36363636;
    opad[i] = bkey[i] ^ 0x5C5C5C5C;
  }

  var hash = binb_sha1(ipad.concat(rstr2binb(data)), 512 + data.length * 8);
  return binb2rstr(binb_sha1(opad.concat(hash), 512 + 160));
}

/*
 * Convert a raw string to a hex string
 */
function rstr2hex(input)
{
  // try { hexcase } catch(e) { hexcase=0; }
  var hex_tab = hexcase ? "0123456789ABCDEF" : "0123456789abcdef";
  var output = "";
  var x;
  for(var i = 0; i < input.length; i++)
  {
    x = input.charCodeAt(i);
    output += hex_tab.charAt((x >>> 4) & 0x0F)
           +  hex_tab.charAt( x        & 0x0F);
  }
  return output;
}

/*
 * Convert a raw string to a base-64 string
 */
function rstr2b64(input)
{
  // try { b64pad } catch(e) { b64pad=''; }
  var tab = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
  var output = "";
  var len = input.length;
  for(var i = 0; i < len; i += 3)
  {
    var triplet = (input.charCodeAt(i) << 16)
                | (i + 1 < len ? input.charCodeAt(i+1) << 8 : 0)
                | (i + 2 < len ? input.charCodeAt(i+2)      : 0);
    for(var j = 0; j < 4; j++)
    {
      if(i * 8 + j * 6 > input.length * 8) output += b64pad;
      else output += tab.charAt((triplet >>> 6*(3-j)) & 0x3F);
    }
  }
  return output;
}

/*
 * Convert a raw string to an arbitrary string encoding
 */
function rstr2any(input, encoding)
{
  var divisor = encoding.length;
  var remainders = Array();
  var i, q, x, quotient;

  /* Convert to an array of 16-bit big-endian values, forming the dividend */
  var dividend = Array(Math.ceil(input.length / 2));
  for(i = 0; i < dividend.length; i++)
  {
    dividend[i] = (input.charCodeAt(i * 2) << 8) | input.charCodeAt(i * 2 + 1);
  }

  /*
   * Repeatedly perform a long division. The binary array forms the dividend,
   * the length of the encoding is the divisor. Once computed, the quotient
   * forms the dividend for the next step. We stop when the dividend is zero.
   * All remainders are stored for later use.
   */
  while(dividend.length > 0)
  {
    quotient = Array();
    x = 0;
    for(i = 0; i < dividend.length; i++)
    {
      x = (x << 16) + dividend[i];
      q = Math.floor(x / divisor);
      x -= q * divisor;
      if(quotient.length > 0 || q > 0)
        quotient[quotient.length] = q;
    }
    remainders[remainders.length] = x;
    dividend = quotient;
  }

  /* Convert the remainders to the output string */
  var output = "";
  for(i = remainders.length - 1; i >= 0; i--)
    output += encoding.charAt(remainders[i]);

  /* Append leading zero equivalents */
  var full_length = Math.ceil(input.length * 8 /
                                    (Math.log(encoding.length) / Math.log(2)))
  for(i = output.length; i < full_length; i++)
    output = encoding[0] + output;

  return output;
}

/*
 * Encode a string as utf-8.
 * For efficiency, this assumes the input is valid utf-16.
 */
function str2rstr_utf8(input)
{
  var output = "";
  var i = -1;
  var x, y;

  while(++i < input.length)
  {
    /* Decode utf-16 surrogate pairs */
    x = input.charCodeAt(i);
    y = i + 1 < input.length ? input.charCodeAt(i + 1) : 0;
    if(0xD800 <= x && x <= 0xDBFF && 0xDC00 <= y && y <= 0xDFFF)
    {
      x = 0x10000 + ((x & 0x03FF) << 10) + (y & 0x03FF);
      i++;
    }

    /* Encode output as utf-8 */
    if(x <= 0x7F)
      output += String.fromCharCode(x);
    else if(x <= 0x7FF)
      output += String.fromCharCode(0xC0 | ((x >>> 6 ) & 0x1F),
                                    0x80 | ( x         & 0x3F));
    else if(x <= 0xFFFF)
      output += String.fromCharCode(0xE0 | ((x >>> 12) & 0x0F),
                                    0x80 | ((x >>> 6 ) & 0x3F),
                                    0x80 | ( x         & 0x3F));
    else if(x <= 0x1FFFFF)
      output += String.fromCharCode(0xF0 | ((x >>> 18) & 0x07),
                                    0x80 | ((x >>> 12) & 0x3F),
                                    0x80 | ((x >>> 6 ) & 0x3F),
                                    0x80 | ( x         & 0x3F));
  }
  return output;
}

/*
 * Encode a string as utf-16
 */
function str2rstr_utf16le(input)
{
  var output = "";
  for(var i = 0; i < input.length; i++)
    output += String.fromCharCode( input.charCodeAt(i)        & 0xFF,
                                  (input.charCodeAt(i) >>> 8) & 0xFF);
  return output;
}

function str2rstr_utf16be(input)
{
  var output = "";
  for(var i = 0; i < input.length; i++)
    output += String.fromCharCode((input.charCodeAt(i) >>> 8) & 0xFF,
                                   input.charCodeAt(i)        & 0xFF);
  return output;
}

/*
 * Convert a raw string to an array of big-endian words
 * Characters >255 have their high-byte silently ignored.
 */
function rstr2binb(input)
{
  var output = Array(input.length >> 2);
  for(var i = 0; i < output.length; i++)
    output[i] = 0;
  for(var i = 0; i < input.length * 8; i += 8)
    output[i>>5] |= (input.charCodeAt(i / 8) & 0xFF) << (24 - i % 32);
  return output;
}

/*
 * Convert an array of big-endian words to a string
 */
function binb2rstr(input)
{
  var output = "";
  for(var i = 0; i < input.length * 32; i += 8)
    output += String.fromCharCode((input[i>>5] >>> (24 - i % 32)) & 0xFF);
  return output;
}

/*
 * Calculate the SHA-1 of an array of big-endian words, and a bit length
 */
function binb_sha1(x, len)
{
  /* append padding */
  x[len >> 5] |= 0x80 << (24 - len % 32);
  x[((len + 64 >> 9) << 4) + 15] = len;

  var w = Array(80);
  var a =  1732584193;
  var b = -271733879;
  var c = -1732584194;
  var d =  271733878;
  var e = -1009589776;

  for(var i = 0; i < x.length; i += 16)
  {
    var olda = a;
    var oldb = b;
    var oldc = c;
    var oldd = d;
    var olde = e;

    for(var j = 0; j < 80; j++)
    {
      if(j < 16) w[j] = x[i + j];
      else w[j] = bit_rol(w[j-3] ^ w[j-8] ^ w[j-14] ^ w[j-16], 1);
      var t = safe_add(safe_add(bit_rol(a, 5), sha1_ft(j, b, c, d)),
                       safe_add(safe_add(e, w[j]), sha1_kt(j)));
      e = d;
      d = c;
      c = bit_rol(b, 30);
      b = a;
      a = t;
    }

    a = safe_add(a, olda);
    b = safe_add(b, oldb);
    c = safe_add(c, oldc);
    d = safe_add(d, oldd);
    e = safe_add(e, olde);
  }
  return Array(a, b, c, d, e);

}

/*
 * Perform the appropriate triplet combination function for the current
 * iteration
 */
function sha1_ft(t, b, c, d)
{
  if(t < 20) return (b & c) | ((~b) & d);
  if(t < 40) return b ^ c ^ d;
  if(t < 60) return (b & c) | (b & d) | (c & d);
  return b ^ c ^ d;
}

/*
 * Determine the appropriate additive constant for the current iteration
 */
function sha1_kt(t)
{
  return (t < 20) ?  1518500249 : (t < 40) ?  1859775393 :
         (t < 60) ? -1894007588 : -899497514;
}

/*
 * Add integers, wrapping at 2^32. This uses 16-bit operations internally
 * to work around bugs in some JS interpreters.
 */
function safe_add(x, y)
{
  var lsw = (x & 0xFFFF) + (y & 0xFFFF);
  var msw = (x >> 16) + (y >> 16) + (lsw >> 16);
  return (msw << 16) | (lsw & 0xFFFF);
}

/*
 * Bitwise rotate a 32-bit number to the left.
 */
function bit_rol(num, cnt)
{
  return (num << cnt) | (num >>> (32 - cnt));
}

if (typeof(module) !== 'undefined') {
  module.exports = {
    b64_sha1: b64_sha1,
    hex_sha1: hex_sha1
  }
}

},{}],9:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// spans.js: JavaScript Intset/Location port.
//

"use strict";


function Range(min, max)
{
    if (typeof(min) != 'number' || typeof(max) != 'number')
        throw 'Bad range ' + min + ',' + max;
    this._min = min;
    this._max = max;
}

Range.prototype.min = function() {
    return this._min;
}

Range.prototype.max = function() {
    return this._max;
}

Range.prototype.contains = function(pos) {
    return pos >= this._min && pos <= this._max;
}

Range.prototype.isContiguous = function() {
    return true;
}

Range.prototype.ranges = function() {
    return [this];
}

Range.prototype._pushRanges = function(ranges) {
    ranges.push(this);
}

Range.prototype.toString = function() {
    return '[' + this._min + '-' + this._max + ']';
}

function _Compound(ranges) {
    this._ranges = ranges;
    // assert sorted?
}

_Compound.prototype.min = function() {
    return this._ranges[0].min();
}

_Compound.prototype.max = function() {
    return this._ranges[this._ranges.length - 1].max();
}

_Compound.prototype.contains = function(pos) {
    // FIXME implement bsearch if we use this much.
    for (var s = 0; s < this._ranges.length; ++s) {
        if (this._ranges[s].contains(pos)) {
            return true;
        }
    }
    return false;
}

_Compound.prototype.isContiguous = function() {
    return this._ranges.length > 1;
}

_Compound.prototype.ranges = function() {
    return this._ranges;
}

_Compound.prototype._pushRanges = function(ranges) {
    for (var ri = 0; ri < this._ranges.length; ++ri)
        ranges.push(this._ranges[ri]);
}

_Compound.prototype.toString = function() {
    var s = '';
    for (var r = 0; r < this._ranges.length; ++r) {
        if (r>0) {
            s = s + ',';
        }
        s = s + this._ranges[r].toString();
    }
    return s;
}

function union(s0, s1) {
    if (! (s0 instanceof Array)) {
        s0 = [s0];
        if (s1)
            s0.push(s1);
    }

    if (s0.length == 0)
        return null;
    else if (s0.length == 1)
        return s0[0];

    var ranges = [];
    for (var si = 0; si < s0.length; ++si)
        s0[si]._pushRanges(ranges);
    ranges = ranges.sort(_rangeOrder);

    var oranges = [];
    var current = ranges[0];
    current = new Range(current._min, current._max);  // Copy now so we don't have to later.

    for (var i = 1; i < ranges.length; ++i) {
        var nxt = ranges[i];
        if (nxt._min > (current._max + 1)) {
            oranges.push(current);
            current = new Range(nxt._min, nxt._max);
        } else {
            if (nxt._max > current._max) {
                current._max = nxt._max;
            }
        }
    }
    oranges.push(current);

    if (oranges.length == 1) {
        return oranges[0];
    } else {
        return new _Compound(oranges);
    }
}

function intersection(s0, s1) {
    var r0 = s0.ranges();
    var r1 = s1.ranges();
    var l0 = r0.length, l1 = r1.length;
    var i0 = 0, i1 = 0;
    var or = [];

    while (i0 < l0 && i1 < l1) {
        var s0 = r0[i0], s1 = r1[i1];
        var lapMin = Math.max(s0.min(), s1.min());
        var lapMax = Math.min(s0.max(), s1.max());
        if (lapMax >= lapMin) {
            or.push(new Range(lapMin, lapMax));
        }
        if (s0.max() > s1.max()) {
            ++i1;
        } else {
            ++i0;
        }
    }
    
    if (or.length == 0) {
        return null; // FIXME
    } else if (or.length == 1) {
        return or[0];
    } else {
        return new _Compound(or);
    }
}

function coverage(s) {
    var tot = 0;
    var rl = s.ranges();
    for (var ri = 0; ri < rl.length; ++ri) {
        var r = rl[ri];
        tot += (r.max() - r.min() + 1);
    }
    return tot;
}



function rangeOrder(a, b)
{
    if (a.min() < b.min()) {
        return -1;
    } else if (a.min() > b.min()) {
        return 1;
    } else if (a.max() < b.max()) {
        return -1;
    } else if (b.max() > a.max()) {
        return 1;
    } else {
        return 0;
    }
}

function _rangeOrder(a, b)
{
    if (a._min < b._min) {
        return -1;
    } else if (a._min > b._min) {
        return 1;
    } else if (a._max < b._max) {
        return -1;
    } else if (b._max > a._max) {
        return 1;
    } else {
        return 0;
    }
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        Range: Range,
        union: union,
        intersection: intersection,
        coverage: coverage,
        rangeOver: rangeOrder,
        _rangeOrder: _rangeOrder
    }
}
},{}],10:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Dalliance Genome Explorer
// (c) Thomas Down 2006-2010
//
// utils.js: odds, sods, and ends.
//

"use strict";

if (typeof(require) !== 'undefined') {
    var sha1 = require('./sha1');
    var b64_sha1 = sha1.b64_sha1;
}

var NUM_REGEXP = new RegExp('[0-9]+');

function stringToNumbersArray(str) {
    var nums = new Array();
    var m;
    while (m = NUM_REGEXP.exec(str)) {
        nums.push(m[0]);
        str=str.substring(m.index + (m[0].length));
    }
    return nums;
}

var STRICT_NUM_REGEXP = new RegExp('^[0-9]+$');

function stringToInt(str) {
    str = str.replace(new RegExp(',', 'g'), '');
    if (!STRICT_NUM_REGEXP.test(str)) {
        return null;
    }
    return str|0;
}

function pushnew(a, v) {
    for (var i = 0; i < a.length; ++i) {
        if (a[i] == v) {
            return;
        }
    }
    a.push(v);
}

function pusho(obj, k, v) {
    if (obj[k]) {
        obj[k].push(v);
    } else {
        obj[k] = [v];
    }
}

function pushnewo(obj, k, v) {
    var a = obj[k];
    if (a) {
        for (var i = 0; i < a.length; ++i) {    // indexOf requires JS16 :-(.
            if (a[i] == v) {
                return;
            }
        }
        a.push(v);
    } else {
        obj[k] = [v];
    }
}


function pick(a, b, c, d)
{
    if (a) {
        return a;
    } else if (b) {
        return b;
    } else if (c) {
        return c;
    } else if (d) {
        return d;
    }
}

function pushnew(l, o)
{
    for (var i = 0; i < l.length; ++i) {
        if (l[i] == o) {
            return;
        }
    }
    l.push(o);
}



function arrayIndexOf(a, x) {
    if (!a) {
        return -1;
    }

    for (var i = 0; i < a.length; ++i) {
        if (a[i] === x) {
            return i;
        }
    }
    return -1;
}

function arrayRemove(a, x) {
    var i = arrayIndexOf(a, x);
    if (i >= 0) {
        a.splice(i, 1);
        return true;
    }
    return false;
}

//
// DOM utilities
//


function makeElement(tag, children, attribs, styles)
{
    var ele = document.createElement(tag);
    if (children) {
        if (! (children instanceof Array)) {
            children = [children];
        }
        for (var i = 0; i < children.length; ++i) {
            var c = children[i];
            if (c) {
                if (typeof c == 'string') {
                    c = document.createTextNode(c);
                } else if (typeof c == 'number') {
                    c = document.createTextNode('' + c);
                }
                ele.appendChild(c);
            }
        }
    }
    
    if (attribs) {
        for (var l in attribs) {
            try {
                ele[l] = attribs[l];
            } catch (e) {
                console.log('error setting ' + l);
                throw(e);
            }
        }
    }
    if (styles) {
        for (var l in styles) {
            ele.style[l] = styles[l];
        }
    }
    return ele;
}

function makeElementNS(namespace, tag, children, attribs)
{
    var ele = document.createElementNS(namespace, tag);
    if (children) {
        if (! (children instanceof Array)) {
            children = [children];
        }
        for (var i = 0; i < children.length; ++i) {
            var c = children[i];
            if (typeof c == 'string') {
                c = document.createTextNode(c);
            }
            ele.appendChild(c);
        }
    }
    
    setAttrs(ele, attribs);
    return ele;
}

var attr_name_cache = {};

function setAttr(node, key, value)
{
    var attr = attr_name_cache[key];
    if (!attr) {
        var _attr = '';
        for (var c = 0; c < key.length; ++c) {
            var cc = key.substring(c, c+1);
            var lcc = cc.toLowerCase();
            if (lcc != cc) {
                _attr = _attr + '-' + lcc;
            } else {
                _attr = _attr + cc;
            }
        }
        attr_name_cache[key] = _attr;
        attr = _attr;
    }
    node.setAttribute(attr, value);
}

function setAttrs(node, attribs)
{
    if (attribs) {
        for (var l in attribs) {
            setAttr(node, l, attribs[l]);
        }
    }
}



function removeChildren(node)
{
    if (!node || !node.childNodes) {
        return;
    }

    while (node.childNodes.length > 0) {
        node.removeChild(node.firstChild);
    }
}



//
// WARNING: not for general use!
//

function miniJSONify(o, exc) {
    if (typeof o === 'undefined') {
        return 'undefined';
    } else if (o == null) {
        return 'null';
    } else if (typeof o == 'string') {
        return "'" + o + "'";
    } else if (typeof o == 'number') {
        return "" + o;
    } else if (typeof o == 'boolean') {
        return "" + o;
    } else if (typeof o == 'object') {
        if (o instanceof Array) {
            var s = null;
            for (var i = 0; i < o.length; ++i) {
                s = (s == null ? '' : (s + ', ')) + miniJSONify(o[i], exc);
            }
            return '[' + (s?s:'') + ']';
        } else {
            exc = exc || {};
            var s = null;
            for (var k in o) {
                if (exc[k])
                    continue;
                if (k != undefined && typeof(o[k]) != 'function') {
                    s = (s == null ? '' : (s + ', ')) + k + ': ' + miniJSONify(o[k], exc);
                }
            }
            return '{' + (s?s:'') + '}';
        }
    } else {
        return (typeof o);
    }
}

function shallowCopy(o) {
    var n = {};
    for (var k in o) {
        n[k] = o[k];
    }
    return n;
}

function Observed(x) {
    this.value = x;
    this.listeners = [];
}

Observed.prototype.addListener = function(f) {
    this.listeners.push(f);
}

Observed.prototype.addListenerAndFire = function(f) {
    this.listeners.push(f);
    f(this.value);
}

Observed.prototype.removeListener = function(f) {
    arrayRemove(this.listeners, f);
}

Observed.prototype.get = function() {
    return this.value;
}

Observed.prototype.set = function(x) {
    this.value = x;
    for (var i = 0; i < this.listeners.length; ++i) {
        this.listeners[i](x);
    }
}

function Awaited() {
    this.queue = [];
}

Awaited.prototype.provide = function(x) {
    if (this.res !== undefined) {
        throw "Resource has already been provided.";
    }

    this.res = x;
    for (var i = 0; i < this.queue.length; ++i) {
        this.queue[i](x);
    }
    this.queue = null;   // avoid leaking closures.
}

Awaited.prototype.await = function(f) {
    if (this.res !== undefined) {
        f(this.res);
        return this.res;
    } else {
        this.queue.push(f);
    }
}

var __dalliance_saltSeed = 0;

function saltURL(url) {
    return url + '?salt=' + b64_sha1('' + Date.now() + ',' + (++__dalliance_saltSeed));
}

function textXHR(url, callback, opts) {
    if (opts.salt) 
        url = saltURL(url);

    var req = new XMLHttpRequest();
    req.onreadystatechange = function() {
    	if (req.readyState == 4) {
    	    if (req.status >= 300) {
    		    callback(null, 'Error code ' + req.status);
    	    } else {
    		    callback(req.responseText);
    	    }
    	}
    };
    
    req.open('GET', url, true);
    req.responseType = 'text';

    if (opts && opts.credentials) {
        req.withCredentials = true;
    }
    req.send('');
}

function relativeURL(base, rel) {
    // FIXME quite naive -- good enough for trackhubs?

    if (rel.indexOf('http:') == 0 || rel.indexOf('https:') == 0) {
        return rel;
    }

    var li = base.lastIndexOf('/');
    if (li >= 0) {
        return base.substr(0, li + 1) + rel;
    } else {
        return rel;
    }
}

var AMINO_ACID_TRANSLATION = {
    'TTT': 'F',
    'TTC': 'F',
    'TTA': 'L',
    'TTG': 'L',
    'CTT': 'L',
    'CTC': 'L',
    'CTA': 'L',
    'CTG': 'L',
    'ATT': 'I',
    'ATC': 'I',
    'ATA': 'I',
    'ATG': 'M',
    'GTT': 'V',
    'GTC': 'V',
    'GTA': 'V',
    'GTG': 'V',
    'TCT': 'S',
    'TCC': 'S',
    'TCA': 'S',
    'TCG': 'S',
    'CCT': 'P',
    'CCC': 'P',
    'CCA': 'P',
    'CCG': 'P',
    'ACT': 'T',
    'ACC': 'T',
    'ACA': 'T',
    'ACG': 'T',
    'GCT': 'A',
    'GCC': 'A',
    'GCA': 'A',
    'GCG': 'A',
    'TAT': 'Y',
    'TAC': 'Y',
    'TAA': '*',  // stop
    'TAG': '*',  // stop
    'CAT': 'H',
    'CAC': 'H',
    'CAA': 'Q',
    'CAG': 'Q',
    'AAT': 'N',
    'AAC': 'N',
    'AAA': 'K',
    'AAG': 'K',
    'GAT': 'D',
    'GAC': 'D',
    'GAA': 'E',
    'GAG': 'E',
    'TGT': 'C',
    'TGC': 'C',
    'TGA': '*',  // stop
    'TGG': 'W',
    'CGT': 'R',
    'CGC': 'R',
    'CGA': 'R',
    'CGG': 'R',
    'AGT': 'S',
    'AGC': 'S',
    'AGA': 'R',
    'AGG': 'R',
    'GGT': 'G',
    'GGC': 'G',
    'GGA': 'G',
    'GGG': 'G'
}

function resolveUrlToPage(rel) {
    return makeElement('a', null, {href: rel}).href;
}

//
// Missing APIs
// 

if (!('trim' in String.prototype)) {
    String.prototype.trim = function() {
        return this.replace(/^\s+/, '').replace(/\s+$/, '');
    };
}

if (typeof(module) !== 'undefined') {
    module.exports = {
        textXHR: textXHR,
        relativeURL: relativeURL,
        resolveUrlToPage: resolveUrlToPage,
        shallowCopy: shallowCopy,
        pusho: pusho,
        pushnew: pushnew,
        pushnewo: pushnewo,
        arrayIndexOf: arrayIndexOf,
        pick: pick,

        makeElement: makeElement,
        makeElementNS: makeElementNS,
        removeChildren: removeChildren,

        miniJSONify: miniJSONify,

        Observed: Observed,
        Awaited: Awaited,

        AMINO_ACID_TRANSLATION: AMINO_ACID_TRANSLATION
    }
}

},{"./sha1":8}],11:[function(require,module,exports){
/* -*- mode: javascript; c-basic-offset: 4; indent-tabs-mode: nil -*- */

// 
// Javascript ZLib
// By Thomas Down 2010-2011
//
// Based very heavily on portions of jzlib (by ymnk@jcraft.com), who in
// turn credits Jean-loup Gailly and Mark Adler for the original zlib code.
//
// inflate.js: ZLib inflate code
//

//
// Shared constants
//

var MAX_WBITS=15; // 32K LZ77 window
var DEF_WBITS=MAX_WBITS;
var MAX_MEM_LEVEL=9;
var MANY=1440;
var BMAX = 15;

// preset dictionary flag in zlib header
var PRESET_DICT=0x20;

var Z_NO_FLUSH=0;
var Z_PARTIAL_FLUSH=1;
var Z_SYNC_FLUSH=2;
var Z_FULL_FLUSH=3;
var Z_FINISH=4;

var Z_DEFLATED=8;

var Z_OK=0;
var Z_STREAM_END=1;
var Z_NEED_DICT=2;
var Z_ERRNO=-1;
var Z_STREAM_ERROR=-2;
var Z_DATA_ERROR=-3;
var Z_MEM_ERROR=-4;
var Z_BUF_ERROR=-5;
var Z_VERSION_ERROR=-6;

var METHOD=0;   // waiting for method byte
var FLAG=1;     // waiting for flag byte
var DICT4=2;    // four dictionary check bytes to go
var DICT3=3;    // three dictionary check bytes to go
var DICT2=4;    // two dictionary check bytes to go
var DICT1=5;    // one dictionary check byte to go
var DICT0=6;    // waiting for inflateSetDictionary
var BLOCKS=7;   // decompressing blocks
var CHECK4=8;   // four check bytes to go
var CHECK3=9;   // three check bytes to go
var CHECK2=10;  // two check bytes to go
var CHECK1=11;  // one check byte to go
var DONE=12;    // finished check, done
var BAD=13;     // got an error--stay here

var inflate_mask = [0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f, 0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff, 0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff, 0x00007fff, 0x0000ffff];

var IB_TYPE=0;  // get type bits (3, including end bit)
var IB_LENS=1;  // get lengths for stored
var IB_STORED=2;// processing stored block
var IB_TABLE=3; // get table lengths
var IB_BTREE=4; // get bit lengths tree for a dynamic block
var IB_DTREE=5; // get length, distance trees for a dynamic block
var IB_CODES=6; // processing fixed or dynamic block
var IB_DRY=7;   // output remaining window bytes
var IB_DONE=8;  // finished last block, done
var IB_BAD=9;   // ot a data error--stuck here

var fixed_bl = 9;
var fixed_bd = 5;

var fixed_tl = [
    96,7,256, 0,8,80, 0,8,16, 84,8,115,
    82,7,31, 0,8,112, 0,8,48, 0,9,192,
    80,7,10, 0,8,96, 0,8,32, 0,9,160,
    0,8,0, 0,8,128, 0,8,64, 0,9,224,
    80,7,6, 0,8,88, 0,8,24, 0,9,144,
    83,7,59, 0,8,120, 0,8,56, 0,9,208,
    81,7,17, 0,8,104, 0,8,40, 0,9,176,
    0,8,8, 0,8,136, 0,8,72, 0,9,240,
    80,7,4, 0,8,84, 0,8,20, 85,8,227,
    83,7,43, 0,8,116, 0,8,52, 0,9,200,
    81,7,13, 0,8,100, 0,8,36, 0,9,168,
    0,8,4, 0,8,132, 0,8,68, 0,9,232,
    80,7,8, 0,8,92, 0,8,28, 0,9,152,
    84,7,83, 0,8,124, 0,8,60, 0,9,216,
    82,7,23, 0,8,108, 0,8,44, 0,9,184,
    0,8,12, 0,8,140, 0,8,76, 0,9,248,
    80,7,3, 0,8,82, 0,8,18, 85,8,163,
    83,7,35, 0,8,114, 0,8,50, 0,9,196,
    81,7,11, 0,8,98, 0,8,34, 0,9,164,
    0,8,2, 0,8,130, 0,8,66, 0,9,228,
    80,7,7, 0,8,90, 0,8,26, 0,9,148,
    84,7,67, 0,8,122, 0,8,58, 0,9,212,
    82,7,19, 0,8,106, 0,8,42, 0,9,180,
    0,8,10, 0,8,138, 0,8,74, 0,9,244,
    80,7,5, 0,8,86, 0,8,22, 192,8,0,
    83,7,51, 0,8,118, 0,8,54, 0,9,204,
    81,7,15, 0,8,102, 0,8,38, 0,9,172,
    0,8,6, 0,8,134, 0,8,70, 0,9,236,
    80,7,9, 0,8,94, 0,8,30, 0,9,156,
    84,7,99, 0,8,126, 0,8,62, 0,9,220,
    82,7,27, 0,8,110, 0,8,46, 0,9,188,
    0,8,14, 0,8,142, 0,8,78, 0,9,252,
    96,7,256, 0,8,81, 0,8,17, 85,8,131,
    82,7,31, 0,8,113, 0,8,49, 0,9,194,
    80,7,10, 0,8,97, 0,8,33, 0,9,162,
    0,8,1, 0,8,129, 0,8,65, 0,9,226,
    80,7,6, 0,8,89, 0,8,25, 0,9,146,
    83,7,59, 0,8,121, 0,8,57, 0,9,210,
    81,7,17, 0,8,105, 0,8,41, 0,9,178,
    0,8,9, 0,8,137, 0,8,73, 0,9,242,
    80,7,4, 0,8,85, 0,8,21, 80,8,258,
    83,7,43, 0,8,117, 0,8,53, 0,9,202,
    81,7,13, 0,8,101, 0,8,37, 0,9,170,
    0,8,5, 0,8,133, 0,8,69, 0,9,234,
    80,7,8, 0,8,93, 0,8,29, 0,9,154,
    84,7,83, 0,8,125, 0,8,61, 0,9,218,
    82,7,23, 0,8,109, 0,8,45, 0,9,186,
    0,8,13, 0,8,141, 0,8,77, 0,9,250,
    80,7,3, 0,8,83, 0,8,19, 85,8,195,
    83,7,35, 0,8,115, 0,8,51, 0,9,198,
    81,7,11, 0,8,99, 0,8,35, 0,9,166,
    0,8,3, 0,8,131, 0,8,67, 0,9,230,
    80,7,7, 0,8,91, 0,8,27, 0,9,150,
    84,7,67, 0,8,123, 0,8,59, 0,9,214,
    82,7,19, 0,8,107, 0,8,43, 0,9,182,
    0,8,11, 0,8,139, 0,8,75, 0,9,246,
    80,7,5, 0,8,87, 0,8,23, 192,8,0,
    83,7,51, 0,8,119, 0,8,55, 0,9,206,
    81,7,15, 0,8,103, 0,8,39, 0,9,174,
    0,8,7, 0,8,135, 0,8,71, 0,9,238,
    80,7,9, 0,8,95, 0,8,31, 0,9,158,
    84,7,99, 0,8,127, 0,8,63, 0,9,222,
    82,7,27, 0,8,111, 0,8,47, 0,9,190,
    0,8,15, 0,8,143, 0,8,79, 0,9,254,
    96,7,256, 0,8,80, 0,8,16, 84,8,115,
    82,7,31, 0,8,112, 0,8,48, 0,9,193,

    80,7,10, 0,8,96, 0,8,32, 0,9,161,
    0,8,0, 0,8,128, 0,8,64, 0,9,225,
    80,7,6, 0,8,88, 0,8,24, 0,9,145,
    83,7,59, 0,8,120, 0,8,56, 0,9,209,
    81,7,17, 0,8,104, 0,8,40, 0,9,177,
    0,8,8, 0,8,136, 0,8,72, 0,9,241,
    80,7,4, 0,8,84, 0,8,20, 85,8,227,
    83,7,43, 0,8,116, 0,8,52, 0,9,201,
    81,7,13, 0,8,100, 0,8,36, 0,9,169,
    0,8,4, 0,8,132, 0,8,68, 0,9,233,
    80,7,8, 0,8,92, 0,8,28, 0,9,153,
    84,7,83, 0,8,124, 0,8,60, 0,9,217,
    82,7,23, 0,8,108, 0,8,44, 0,9,185,
    0,8,12, 0,8,140, 0,8,76, 0,9,249,
    80,7,3, 0,8,82, 0,8,18, 85,8,163,
    83,7,35, 0,8,114, 0,8,50, 0,9,197,
    81,7,11, 0,8,98, 0,8,34, 0,9,165,
    0,8,2, 0,8,130, 0,8,66, 0,9,229,
    80,7,7, 0,8,90, 0,8,26, 0,9,149,
    84,7,67, 0,8,122, 0,8,58, 0,9,213,
    82,7,19, 0,8,106, 0,8,42, 0,9,181,
    0,8,10, 0,8,138, 0,8,74, 0,9,245,
    80,7,5, 0,8,86, 0,8,22, 192,8,0,
    83,7,51, 0,8,118, 0,8,54, 0,9,205,
    81,7,15, 0,8,102, 0,8,38, 0,9,173,
    0,8,6, 0,8,134, 0,8,70, 0,9,237,
    80,7,9, 0,8,94, 0,8,30, 0,9,157,
    84,7,99, 0,8,126, 0,8,62, 0,9,221,
    82,7,27, 0,8,110, 0,8,46, 0,9,189,
    0,8,14, 0,8,142, 0,8,78, 0,9,253,
    96,7,256, 0,8,81, 0,8,17, 85,8,131,
    82,7,31, 0,8,113, 0,8,49, 0,9,195,
    80,7,10, 0,8,97, 0,8,33, 0,9,163,
    0,8,1, 0,8,129, 0,8,65, 0,9,227,
    80,7,6, 0,8,89, 0,8,25, 0,9,147,
    83,7,59, 0,8,121, 0,8,57, 0,9,211,
    81,7,17, 0,8,105, 0,8,41, 0,9,179,
    0,8,9, 0,8,137, 0,8,73, 0,9,243,
    80,7,4, 0,8,85, 0,8,21, 80,8,258,
    83,7,43, 0,8,117, 0,8,53, 0,9,203,
    81,7,13, 0,8,101, 0,8,37, 0,9,171,
    0,8,5, 0,8,133, 0,8,69, 0,9,235,
    80,7,8, 0,8,93, 0,8,29, 0,9,155,
    84,7,83, 0,8,125, 0,8,61, 0,9,219,
    82,7,23, 0,8,109, 0,8,45, 0,9,187,
    0,8,13, 0,8,141, 0,8,77, 0,9,251,
    80,7,3, 0,8,83, 0,8,19, 85,8,195,
    83,7,35, 0,8,115, 0,8,51, 0,9,199,
    81,7,11, 0,8,99, 0,8,35, 0,9,167,
    0,8,3, 0,8,131, 0,8,67, 0,9,231,
    80,7,7, 0,8,91, 0,8,27, 0,9,151,
    84,7,67, 0,8,123, 0,8,59, 0,9,215,
    82,7,19, 0,8,107, 0,8,43, 0,9,183,
    0,8,11, 0,8,139, 0,8,75, 0,9,247,
    80,7,5, 0,8,87, 0,8,23, 192,8,0,
    83,7,51, 0,8,119, 0,8,55, 0,9,207,
    81,7,15, 0,8,103, 0,8,39, 0,9,175,
    0,8,7, 0,8,135, 0,8,71, 0,9,239,
    80,7,9, 0,8,95, 0,8,31, 0,9,159,
    84,7,99, 0,8,127, 0,8,63, 0,9,223,
    82,7,27, 0,8,111, 0,8,47, 0,9,191,
    0,8,15, 0,8,143, 0,8,79, 0,9,255
];
var fixed_td = [
    80,5,1, 87,5,257, 83,5,17, 91,5,4097,
    81,5,5, 89,5,1025, 85,5,65, 93,5,16385,
    80,5,3, 88,5,513, 84,5,33, 92,5,8193,
    82,5,9, 90,5,2049, 86,5,129, 192,5,24577,
    80,5,2, 87,5,385, 83,5,25, 91,5,6145,
    81,5,7, 89,5,1537, 85,5,97, 93,5,24577,
    80,5,4, 88,5,769, 84,5,49, 92,5,12289,
    82,5,13, 90,5,3073, 86,5,193, 192,5,24577
];

  // Tables for deflate from PKZIP's appnote.txt.
  var cplens = [ // Copy lengths for literal codes 257..285
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
  ];

  // see note #13 above about 258
  var cplext = [ // Extra bits for literal codes 257..285
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 112, 112  // 112==invalid
  ];

 var cpdist = [ // Copy offsets for distance codes 0..29
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
        8193, 12289, 16385, 24577
  ];

  var cpdext = [ // Extra bits for distance codes
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11,
        12, 12, 13, 13];

//
// ZStream.java
//

function ZStream() {
}


ZStream.prototype.inflateInit = function(w, nowrap) {
    if (!w) {
	w = DEF_WBITS;
    }
    if (nowrap) {
	nowrap = false;
    }
    this.istate = new Inflate();
    return this.istate.inflateInit(this, nowrap?-w:w);
}

ZStream.prototype.inflate = function(f) {
    if(this.istate==null) return Z_STREAM_ERROR;
    return this.istate.inflate(this, f);
}

ZStream.prototype.inflateEnd = function(){
    if(this.istate==null) return Z_STREAM_ERROR;
    var ret=istate.inflateEnd(this);
    this.istate = null;
    return ret;
}
ZStream.prototype.inflateSync = function(){
    // if(istate == null) return Z_STREAM_ERROR;
    return istate.inflateSync(this);
}
ZStream.prototype.inflateSetDictionary = function(dictionary, dictLength){
    // if(istate == null) return Z_STREAM_ERROR;
    return istate.inflateSetDictionary(this, dictionary, dictLength);
}

/*

  public int deflateInit(int level){
    return deflateInit(level, MAX_WBITS);
  }
  public int deflateInit(int level, boolean nowrap){
    return deflateInit(level, MAX_WBITS, nowrap);
  }
  public int deflateInit(int level, int bits){
    return deflateInit(level, bits, false);
  }
  public int deflateInit(int level, int bits, boolean nowrap){
    dstate=new Deflate();
    return dstate.deflateInit(this, level, nowrap?-bits:bits);
  }
  public int deflate(int flush){
    if(dstate==null){
      return Z_STREAM_ERROR;
    }
    return dstate.deflate(this, flush);
  }
  public int deflateEnd(){
    if(dstate==null) return Z_STREAM_ERROR;
    int ret=dstate.deflateEnd();
    dstate=null;
    return ret;
  }
  public int deflateParams(int level, int strategy){
    if(dstate==null) return Z_STREAM_ERROR;
    return dstate.deflateParams(this, level, strategy);
  }
  public int deflateSetDictionary (byte[] dictionary, int dictLength){
    if(dstate == null)
      return Z_STREAM_ERROR;
    return dstate.deflateSetDictionary(this, dictionary, dictLength);
  }

*/

/*
  // Flush as much pending output as possible. All deflate() output goes
  // through this function so some applications may wish to modify it
  // to avoid allocating a large strm->next_out buffer and copying into it.
  // (See also read_buf()).
  void flush_pending(){
    int len=dstate.pending;

    if(len>avail_out) len=avail_out;
    if(len==0) return;

    if(dstate.pending_buf.length<=dstate.pending_out ||
       next_out.length<=next_out_index ||
       dstate.pending_buf.length<(dstate.pending_out+len) ||
       next_out.length<(next_out_index+len)){
      System.out.println(dstate.pending_buf.length+", "+dstate.pending_out+
			 ", "+next_out.length+", "+next_out_index+", "+len);
      System.out.println("avail_out="+avail_out);
    }

    System.arraycopy(dstate.pending_buf, dstate.pending_out,
		     next_out, next_out_index, len);

    next_out_index+=len;
    dstate.pending_out+=len;
    total_out+=len;
    avail_out-=len;
    dstate.pending-=len;
    if(dstate.pending==0){
      dstate.pending_out=0;
    }
  }

  // Read a new buffer from the current input stream, update the adler32
  // and total number of bytes read.  All deflate() input goes through
  // this function so some applications may wish to modify it to avoid
  // allocating a large strm->next_in buffer and copying from it.
  // (See also flush_pending()).
  int read_buf(byte[] buf, int start, int size) {
    int len=avail_in;

    if(len>size) len=size;
    if(len==0) return 0;

    avail_in-=len;

    if(dstate.noheader==0) {
      adler=_adler.adler32(adler, next_in, next_in_index, len);
    }
    System.arraycopy(next_in, next_in_index, buf, start, len);
    next_in_index  += len;
    total_in += len;
    return len;
  }

  public void free(){
    next_in=null;
    next_out=null;
    msg=null;
    _adler=null;
  }
}
*/


//
// Inflate.java
//

function Inflate() {
    this.was = [0];
}

Inflate.prototype.inflateReset = function(z) {
    if(z == null || z.istate == null) return Z_STREAM_ERROR;
    
    z.total_in = z.total_out = 0;
    z.msg = null;
    z.istate.mode = z.istate.nowrap!=0 ? BLOCKS : METHOD;
    z.istate.blocks.reset(z, null);
    return Z_OK;
}

Inflate.prototype.inflateEnd = function(z){
    if(this.blocks != null)
      this.blocks.free(z);
    this.blocks=null;
    return Z_OK;
}

Inflate.prototype.inflateInit = function(z, w){
    z.msg = null;
    this.blocks = null;

    // handle undocumented nowrap option (no zlib header or check)
    nowrap = 0;
    if(w < 0){
      w = - w;
      nowrap = 1;
    }

    // set window size
    if(w<8 ||w>15){
      this.inflateEnd(z);
      return Z_STREAM_ERROR;
    }
    this.wbits=w;

    z.istate.blocks=new InfBlocks(z, 
				  z.istate.nowrap!=0 ? null : this,
				  1<<w);

    // reset state
    this.inflateReset(z);
    return Z_OK;
  }

Inflate.prototype.inflate = function(z, f){
    var r, b;

    if(z == null || z.istate == null || z.next_in == null)
      return Z_STREAM_ERROR;
    f = f == Z_FINISH ? Z_BUF_ERROR : Z_OK;
    r = Z_BUF_ERROR;
    while (true){
      switch (z.istate.mode){
      case METHOD:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        if(((z.istate.method = z.next_in[z.next_in_index++])&0xf)!=Z_DEFLATED){
          z.istate.mode = BAD;
          z.msg="unknown compression method";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }
        if((z.istate.method>>4)+8>z.istate.wbits){
          z.istate.mode = BAD;
          z.msg="invalid window size";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }
        z.istate.mode=FLAG;
      case FLAG:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        b = (z.next_in[z.next_in_index++])&0xff;

        if((((z.istate.method << 8)+b) % 31)!=0){
          z.istate.mode = BAD;
          z.msg = "incorrect header check";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }

        if((b&PRESET_DICT)==0){
          z.istate.mode = BLOCKS;
          break;
        }
        z.istate.mode = DICT4;
      case DICT4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000;
        z.istate.mode=DICT3;
      case DICT3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000;
        z.istate.mode=DICT2;
      case DICT2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00;
        z.istate.mode=DICT1;
      case DICT1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need += (z.next_in[z.next_in_index++]&0xff);
        z.adler = z.istate.need;
        z.istate.mode = DICT0;
        return Z_NEED_DICT;
      case DICT0:
        z.istate.mode = BAD;
        z.msg = "need dictionary";
        z.istate.marker = 0;       // can try inflateSync
        return Z_STREAM_ERROR;
      case BLOCKS:

        r = z.istate.blocks.proc(z, r);
        if(r == Z_DATA_ERROR){
          z.istate.mode = BAD;
          z.istate.marker = 0;     // can try inflateSync
          break;
        }
        if(r == Z_OK){
          r = f;
        }
        if(r != Z_STREAM_END){
          return r;
        }
        r = f;
        z.istate.blocks.reset(z, z.istate.was);
        if(z.istate.nowrap!=0){
          z.istate.mode=DONE;
          break;
        }
        z.istate.mode=CHECK4;
      case CHECK4:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need=((z.next_in[z.next_in_index++]&0xff)<<24)&0xff000000;
        z.istate.mode=CHECK3;
      case CHECK3:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<16)&0xff0000;
        z.istate.mode = CHECK2;
      case CHECK2:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=((z.next_in[z.next_in_index++]&0xff)<<8)&0xff00;
        z.istate.mode = CHECK1;
      case CHECK1:

        if(z.avail_in==0)return r;r=f;

        z.avail_in--; z.total_in++;
        z.istate.need+=(z.next_in[z.next_in_index++]&0xff);

        if(((z.istate.was[0])) != ((z.istate.need))){
          z.istate.mode = BAD;
          z.msg = "incorrect data check";
          z.istate.marker = 5;       // can't try inflateSync
          break;
        }

        z.istate.mode = DONE;
      case DONE:
        return Z_STREAM_END;
      case BAD:
        return Z_DATA_ERROR;
      default:
        return Z_STREAM_ERROR;
      }
    }
  }


Inflate.prototype.inflateSetDictionary = function(z,  dictionary, dictLength) {
    var index=0;
    var length = dictLength;
    if(z==null || z.istate == null|| z.istate.mode != DICT0)
      return Z_STREAM_ERROR;

    if(z._adler.adler32(1, dictionary, 0, dictLength)!=z.adler){
      return Z_DATA_ERROR;
    }

    z.adler = z._adler.adler32(0, null, 0, 0);

    if(length >= (1<<z.istate.wbits)){
      length = (1<<z.istate.wbits)-1;
      index=dictLength - length;
    }
    z.istate.blocks.set_dictionary(dictionary, index, length);
    z.istate.mode = BLOCKS;
    return Z_OK;
  }

//  static private byte[] mark = {(byte)0, (byte)0, (byte)0xff, (byte)0xff};
var mark = [0, 0, 255, 255]

Inflate.prototype.inflateSync = function(z){
    var n;       // number of bytes to look at
    var p;       // pointer to bytes
    var m;       // number of marker bytes found in a row
    var r, w;   // temporaries to save total_in and total_out

    // set up
    if(z == null || z.istate == null)
      return Z_STREAM_ERROR;
    if(z.istate.mode != BAD){
      z.istate.mode = BAD;
      z.istate.marker = 0;
    }
    if((n=z.avail_in)==0)
      return Z_BUF_ERROR;
    p=z.next_in_index;
    m=z.istate.marker;

    // search
    while (n!=0 && m < 4){
      if(z.next_in[p] == mark[m]){
        m++;
      }
      else if(z.next_in[p]!=0){
        m = 0;
      }
      else{
        m = 4 - m;
      }
      p++; n--;
    }

    // restore
    z.total_in += p-z.next_in_index;
    z.next_in_index = p;
    z.avail_in = n;
    z.istate.marker = m;

    // return no joy or set up to restart on a new block
    if(m != 4){
      return Z_DATA_ERROR;
    }
    r=z.total_in;  w=z.total_out;
    this.inflateReset(z);
    z.total_in=r;  z.total_out = w;
    z.istate.mode = BLOCKS;
    return Z_OK;
}

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. This function is used by one PPP
  // implementation to provide an additional safety check. PPP uses Z_SYNC_FLUSH
  // but removes the length bytes of the resulting empty stored block. When
  // decompressing, PPP checks that at the end of input packet, inflate is
  // waiting for these length bytes.
Inflate.prototype.inflateSyncPoint = function(z){
    if(z == null || z.istate == null || z.istate.blocks == null)
      return Z_STREAM_ERROR;
    return z.istate.blocks.sync_point();
}


//
// InfBlocks.java
//

var INFBLOCKS_BORDER = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15];

function InfBlocks(z, checkfn, w) {
    this.hufts=new Int32Array(MANY*3);
    this.window=new Uint8Array(w);
    this.end=w;
    this.checkfn = checkfn;
    this.mode = IB_TYPE;
    this.reset(z, null);

    this.left = 0;            // if STORED, bytes left to copy 

    this.table = 0;           // table lengths (14 bits) 
    this.index = 0;           // index into blens (or border) 
    this.blens = null;         // bit lengths of codes 
    this.bb=new Int32Array(1); // bit length tree depth 
    this.tb=new Int32Array(1); // bit length decoding tree 

    this.codes = new InfCodes();

    this.last = 0;            // true if this block is the last block 

  // mode independent information 
    this.bitk = 0;            // bits in bit buffer 
    this.bitb = 0;            // bit buffer 
    this.read = 0;            // window read pointer 
    this.write = 0;           // window write pointer 
    this.check = 0;          // check on output 

    this.inftree=new InfTree();
}




InfBlocks.prototype.reset = function(z, c){
    if(c) c[0]=this.check;
    if(this.mode==IB_CODES){
      this.codes.free(z);
    }
    this.mode=IB_TYPE;
    this.bitk=0;
    this.bitb=0;
    this.read=this.write=0;

    if(this.checkfn)
      z.adler=this.check=z._adler.adler32(0, null, 0, 0);
  }

 InfBlocks.prototype.proc = function(z, r){
    var t;              // temporary storage
    var b;              // bit buffer
    var k;              // bits in bit buffer
    var p;              // input data pointer
    var n;              // bytes available there
    var q;              // output window write pointer
    var m;              // bytes to end of window or read pointer

    // copy input/output information to locals (UPDATE macro restores)
    {p=z.next_in_index;n=z.avail_in;b=this.bitb;k=this.bitk;}
    {q=this.write;m=(q<this.read ? this.read-q-1 : this.end-q);}

    // process input based on current state
    while(true){
      switch (this.mode){
      case IB_TYPE:

	while(k<(3)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}
	t = (b & 7);
	this.last = t & 1;

	switch (t >>> 1){
        case 0:                         // stored 
          {b>>>=(3);k-=(3);}
          t = k & 7;                    // go to byte boundary

          {b>>>=(t);k-=(t);}
          this.mode = IB_LENS;                  // get length of stored block
          break;
        case 1:                         // fixed
          {
              var bl=new Int32Array(1);
	      var bd=new Int32Array(1);
              var tl=[];
	      var td=[];

	      inflate_trees_fixed(bl, bd, tl, td, z);
              this.codes.init(bl[0], bd[0], tl[0], 0, td[0], 0, z);
          }

          {b>>>=(3);k-=(3);}

          this.mode = IB_CODES;
          break;
        case 2:                         // dynamic

          {b>>>=(3);k-=(3);}

          this.mode = IB_TABLE;
          break;
        case 3:                         // illegal

          {b>>>=(3);k-=(3);}
          this.mode = BAD;
          z.msg = "invalid block type";
          r = Z_DATA_ERROR;

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z,r);
	}
	break;
      case IB_LENS:
	while(k<(32)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	if ((((~b) >>> 16) & 0xffff) != (b & 0xffff)){
	  this.mode = BAD;
	  z.msg = "invalid stored block lengths";
	  r = Z_DATA_ERROR;

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z,r);
	}
	this.left = (b & 0xffff);
	b = k = 0;                       // dump bits
	this.mode = this.left!=0 ? IB_STORED : (this.last!=0 ? IB_DRY : IB_TYPE);
	break;
      case IB_STORED:
	if (n == 0){
	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return this.inflate_flush(z,r);
	}

	if(m==0){
	  if(q==end&&read!=0){
	    q=0; m=(q<this.read ? this.read-q-1 : this.end-q);
	  }
	  if(m==0){
	    this.write=q; 
	    r=this.inflate_flush(z,r);
	    q=this.write; m = (q < this.read ? this.read-q-1 : this.end-q);
	    if(q==this.end && this.read != 0){
	      q=0; m = (q < this.read ? this.read-q-1 : this.end-q);
	    }
	    if(m==0){
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    }
	  }
	}
	r=Z_OK;

	t = this.left;
	if(t>n) t = n;
	if(t>m) t = m;
	arrayCopy(z.next_in, p, this.window, q, t);
	p += t;  n -= t;
	q += t;  m -= t;
	if ((this.left -= t) != 0)
	  break;
	this.mode = (this.last != 0 ? IB_DRY : IB_TYPE);
	break;
      case IB_TABLE:

	while(k<(14)){
	  if(n!=0){
	    r=Z_OK;
	  }
	  else{
	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;
	    z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  };
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.table = t = (b & 0x3fff);
	if ((t & 0x1f) > 29 || ((t >> 5) & 0x1f) > 29)
	  {
	    this.mode = IB_BAD;
	    z.msg = "too many length or distance symbols";
	    r = Z_DATA_ERROR;

	    this.bitb=b; this.bitk=k; 
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    this.write=q;
	    return this.inflate_flush(z,r);
	  }
	t = 258 + (t & 0x1f) + ((t >> 5) & 0x1f);
	if(this.blens==null || this.blens.length<t){
	    this.blens=new Int32Array(t);
	}
	else{
	  for(var i=0; i<t; i++){
              this.blens[i]=0;
          }
	}

	{b>>>=(14);k-=(14);}

	this.index = 0;
	mode = IB_BTREE;
      case IB_BTREE:
	while (this.index < 4 + (this.table >>> 10)){
	  while(k<(3)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    };
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

	  this.blens[INFBLOCKS_BORDER[this.index++]] = b&7;

	  {b>>>=(3);k-=(3);}
	}

	while(this.index < 19){
	  this.blens[INFBLOCKS_BORDER[this.index++]] = 0;
	}

	this.bb[0] = 7;
	t = this.inftree.inflate_trees_bits(this.blens, this.bb, this.tb, this.hufts, z);
	if (t != Z_OK){
	  r = t;
	  if (r == Z_DATA_ERROR){
	    this.blens=null;
	    this.mode = IB_BAD;
	  }

	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  write=q;
	  return this.inflate_flush(z,r);
	}

	this.index = 0;
	this.mode = IB_DTREE;
      case IB_DTREE:
	while (true){
	  t = this.table;
	  if(!(this.index < 258 + (t & 0x1f) + ((t >> 5) & 0x1f))){
	    break;
	  }

	  var h; //int[]
	  var i, j, c;

	  t = this.bb[0];

	  while(k<(t)){
	    if(n!=0){
	      r=Z_OK;
	    }
	    else{
	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;
	      z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    };
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;
	    k+=8;
	  }

//	  if (this.tb[0]==-1){
//            dlog("null...");
//	  }

	  t=this.hufts[(this.tb[0]+(b & inflate_mask[t]))*3+1];
	  c=this.hufts[(this.tb[0]+(b & inflate_mask[t]))*3+2];

	  if (c < 16){
	    b>>>=(t);k-=(t);
	    this.blens[this.index++] = c;
	  }
	  else { // c == 16..18
	    i = c == 18 ? 7 : c - 14;
	    j = c == 18 ? 11 : 3;

	    while(k<(t+i)){
	      if(n!=0){
		r=Z_OK;
	      }
	      else{
		this.bitb=b; this.bitk=k; 
		z.avail_in=n;
		z.total_in+=p-z.next_in_index;z.next_in_index=p;
		this.write=q;
		return this.inflate_flush(z,r);
	      };
	      n--;
	      b|=(z.next_in[p++]&0xff)<<k;
	      k+=8;
	    }

	    b>>>=(t);k-=(t);

	    j += (b & inflate_mask[i]);

	    b>>>=(i);k-=(i);

	    i = this.index;
	    t = this.table;
	    if (i + j > 258 + (t & 0x1f) + ((t >> 5) & 0x1f) ||
		(c == 16 && i < 1)){
	      this.blens=null;
	      this.mode = IB_BAD;
	      z.msg = "invalid bit length repeat";
	      r = Z_DATA_ERROR;

	      this.bitb=b; this.bitk=k; 
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      this.write=q;
	      return this.inflate_flush(z,r);
	    }

	    c = c == 16 ? this.blens[i-1] : 0;
	    do{
	      this.blens[i++] = c;
	    }
	    while (--j!=0);
	    this.index = i;
	  }
	}

	this.tb[0]=-1;
	{
	    var bl=new Int32Array(1);
	    var bd=new Int32Array(1);
	    var tl=new Int32Array(1);
	    var td=new Int32Array(1);
	    bl[0] = 9;         // must be <= 9 for lookahead assumptions
	    bd[0] = 6;         // must be <= 9 for lookahead assumptions

	    t = this.table;
	    t = this.inftree.inflate_trees_dynamic(257 + (t & 0x1f), 
					      1 + ((t >> 5) & 0x1f),
					      this.blens, bl, bd, tl, td, this.hufts, z);

	    if (t != Z_OK){
	        if (t == Z_DATA_ERROR){
	            this.blens=null;
	            this.mode = BAD;
	        }
	        r = t;

	        this.bitb=b; this.bitk=k; 
	        z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	        this.write=q;
	        return this.inflate_flush(z,r);
	    }
	    this.codes.init(bl[0], bd[0], this.hufts, tl[0], this.hufts, td[0], z);
	}
	this.mode = IB_CODES;
      case IB_CODES:
	this.bitb=b; this.bitk=k;
	z.avail_in=n; z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;

	if ((r = this.codes.proc(this, z, r)) != Z_STREAM_END){
	  return this.inflate_flush(z, r);
	}
	r = Z_OK;
	this.codes.free(z);

	p=z.next_in_index; n=z.avail_in;b=this.bitb;k=this.bitk;
	q=this.write;m = (q < this.read ? this.read-q-1 : this.end-q);

	if (this.last==0){
	  this.mode = IB_TYPE;
	  break;
	}
	this.mode = IB_DRY;
      case IB_DRY:
	this.write=q; 
	r = this.inflate_flush(z, r); 
	q=this.write; m = (q < this.read ? this.read-q-1 : this.end-q);
	if (this.read != this.write){
	  this.bitb=b; this.bitk=k; 
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  this.write=q;
	  return this.inflate_flush(z, r);
	}
	mode = DONE;
      case IB_DONE:
	r = Z_STREAM_END;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);
      case IB_BAD:
	r = Z_DATA_ERROR;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);

      default:
	r = Z_STREAM_ERROR;

	this.bitb=b; this.bitk=k; 
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	this.write=q;
	return this.inflate_flush(z, r);
      }
    }
  }

InfBlocks.prototype.free = function(z){
    this.reset(z, null);
    this.window=null;
    this.hufts=null;
}

InfBlocks.prototype.set_dictionary = function(d, start, n){
    arrayCopy(d, start, window, 0, n);
    this.read = this.write = n;
}

  // Returns true if inflate is currently at the end of a block generated
  // by Z_SYNC_FLUSH or Z_FULL_FLUSH. 
InfBlocks.prototype.sync_point = function(){
    return this.mode == IB_LENS;
}

  // copy as much as possible from the sliding window to the output area
InfBlocks.prototype.inflate_flush = function(z, r){
    var n;
    var p;
    var q;

    // local copies of source and destination pointers
    p = z.next_out_index;
    q = this.read;

    // compute number of bytes to copy as far as end of window
    n = ((q <= this.write ? this.write : this.end) - q);
    if (n > z.avail_out) n = z.avail_out;
    if (n!=0 && r == Z_BUF_ERROR) r = Z_OK;

    // update counters
    z.avail_out -= n;
    z.total_out += n;

    // update check information
    if(this.checkfn != null)
      z.adler=this.check=z._adler.adler32(this.check, this.window, q, n);

    // copy as far as end of window
    arrayCopy(this.window, q, z.next_out, p, n);
    p += n;
    q += n;

    // see if more to copy at beginning of window
    if (q == this.end){
      // wrap pointers
      q = 0;
      if (this.write == this.end)
        this.write = 0;

      // compute bytes to copy
      n = this.write - q;
      if (n > z.avail_out) n = z.avail_out;
      if (n!=0 && r == Z_BUF_ERROR) r = Z_OK;

      // update counters
      z.avail_out -= n;
      z.total_out += n;

      // update check information
      if(this.checkfn != null)
	z.adler=this.check=z._adler.adler32(this.check, this.window, q, n);

      // copy
      arrayCopy(this.window, q, z.next_out, p, n);
      p += n;
      q += n;
    }

    // update pointers
    z.next_out_index = p;
    this.read = q;

    // done
    return r;
  }

//
// InfCodes.java
//

var IC_START=0;  // x: set up for LEN
var IC_LEN=1;    // i: get length/literal/eob next
var IC_LENEXT=2; // i: getting length extra (have base)
var IC_DIST=3;   // i: get distance next
var IC_DISTEXT=4;// i: getting distance extra
var IC_COPY=5;   // o: copying bytes in window, waiting for space
var IC_LIT=6;    // o: got literal, waiting for output space
var IC_WASH=7;   // o: got eob, possibly still output waiting
var IC_END=8;    // x: got eob and all data flushed
var IC_BADCODE=9;// x: got error

function InfCodes() {
}

InfCodes.prototype.init = function(bl, bd, tl, tl_index, td, td_index, z) {
    this.mode=IC_START;
    this.lbits=bl;
    this.dbits=bd;
    this.ltree=tl;
    this.ltree_index=tl_index;
    this.dtree = td;
    this.dtree_index=td_index;
    this.tree=null;
}

InfCodes.prototype.proc = function(s, z, r){ 
    var j;              // temporary storage
    var t;              // temporary pointer (int[])
    var tindex;         // temporary pointer
    var e;              // extra bits or operation
    var b=0;            // bit buffer
    var k=0;            // bits in bit buffer
    var p=0;            // input data pointer
    var n;              // bytes available there
    var q;              // output window write pointer
    var m;              // bytes to end of window or read pointer
    var f;              // pointer to copy strings from

    // copy input/output information to locals (UPDATE macro restores)
    p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

    // process input and output based on current state
    while (true){
      switch (this.mode){
	// waiting for "i:"=input, "o:"=output, "x:"=nothing
      case IC_START:         // x: set up for LEN
	if (m >= 258 && n >= 10){

	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;
	  r = this.inflate_fast(this.lbits, this.dbits, 
			   this.ltree, this.ltree_index, 
			   this.dtree, this.dtree_index,
			   s, z);

	  p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
	  q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	  if (r != Z_OK){
	    this.mode = r == Z_STREAM_END ? IC_WASH : IC_BADCODE;
	    break;
	  }
	}
	this.need = this.lbits;
	this.tree = this.ltree;
	this.tree_index=this.ltree_index;

	this.mode = IC_LEN;
      case IC_LEN:           // i: get length/literal/eob next
	j = this.need;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--;
	  b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	tindex=(this.tree_index+(b&inflate_mask[j]))*3;

	b>>>=(this.tree[tindex+1]);
	k-=(this.tree[tindex+1]);

	e=this.tree[tindex];

	if(e == 0){               // literal
	  this.lit = this.tree[tindex+2];
	  this.mode = IC_LIT;
	  break;
	}
	if((e & 16)!=0 ){          // length
	  this.get = e & 15;
	  this.len = this.tree[tindex+2];
	  this.mode = IC_LENEXT;
	  break;
	}
	if ((e & 64) == 0){        // next table
	  this.need = e;
	  this.tree_index = tindex/3 + this.tree[tindex+2];
	  break;
	}
	if ((e & 32)!=0){               // end of block
	  this.mode = IC_WASH;
	  break;
	}
	this.mode = IC_BADCODE;        // invalid code
	z.msg = "invalid literal/length code";
	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_LENEXT:        // i: getting length extra (have base)
	j = this.get;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.len += (b & inflate_mask[j]);

	b>>=j;
	k-=j;

	this.need = this.dbits;
	this.tree = this.dtree;
	this.tree_index = this.dtree_index;
	this.mode = IC_DIST;
      case IC_DIST:          // i: get distance next
	j = this.need;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	tindex=(this.tree_index+(b & inflate_mask[j]))*3;

	b>>=this.tree[tindex+1];
	k-=this.tree[tindex+1];

	e = (this.tree[tindex]);
	if((e & 16)!=0){               // distance
	  this.get = e & 15;
	  this.dist = this.tree[tindex+2];
	  this.mode = IC_DISTEXT;
	  break;
	}
	if ((e & 64) == 0){        // next table
	  this.need = e;
	  this.tree_index = tindex/3 + this.tree[tindex+2];
	  break;
	}
	this.mode = IC_BADCODE;        // invalid code
	z.msg = "invalid distance code";
	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_DISTEXT:       // i: getting distance extra
	j = this.get;

	while(k<(j)){
	  if(n!=0)r=Z_OK;
	  else{

	    s.bitb=b;s.bitk=k;
	    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	    s.write=q;
	    return s.inflate_flush(z,r);
	  }
	  n--; b|=(z.next_in[p++]&0xff)<<k;
	  k+=8;
	}

	this.dist += (b & inflate_mask[j]);

	b>>=j;
	k-=j;

	this.mode = IC_COPY;
      case IC_COPY:          // o: copying bytes in window, waiting for space
        f = q - this.dist;
        while(f < 0){     // modulo window size-"while" instead
          f += s.end;     // of "if" handles invalid distances
	}
	while (this.len!=0){

	  if(m==0){
	    if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	    if(m==0){
	      s.write=q; r=s.inflate_flush(z,r);
	      q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	      if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}

	      if(m==0){
		s.bitb=b;s.bitk=k;
		z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
		s.write=q;
		return s.inflate_flush(z,r);
	      }  
	    }
	  }

	  s.window[q++]=s.window[f++]; m--;

	  if (f == s.end)
            f = 0;
	  this.len--;
	}
	this.mode = IC_START;
	break;
      case IC_LIT:           // o: got literal, waiting for output space
	if(m==0){
	  if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	  if(m==0){
	    s.write=q; r=s.inflate_flush(z,r);
	    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	    if(q==s.end&&s.read!=0){q=0;m=q<s.read?s.read-q-1:s.end-q;}
	    if(m==0){
	      s.bitb=b;s.bitk=k;
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      s.write=q;
	      return s.inflate_flush(z,r);
	    }
	  }
	}
	r=Z_OK;

	s.window[q++]=this.lit; m--;

	this.mode = IC_START;
	break;
      case IC_WASH:           // o: got eob, possibly more output
	if (k > 7){        // return unused byte, if any
	  k -= 8;
	  n++;
	  p--;             // can always return one
	}

	s.write=q; r=s.inflate_flush(z,r);
	q=s.write;m=q<s.read?s.read-q-1:s.end-q;

	if (s.read != s.write){
	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;
	  return s.inflate_flush(z,r);
	}
	this.mode = IC_END;
      case IC_END:
	r = Z_STREAM_END;
	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      case IC_BADCODE:       // x: got error

	r = Z_DATA_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);

      default:
	r = Z_STREAM_ERROR;

	s.bitb=b;s.bitk=k;
	z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	s.write=q;
	return s.inflate_flush(z,r);
      }
    }
  }

InfCodes.prototype.free = function(z){
    //  ZFREE(z, c);
}

  // Called with number of bytes left to write in window at least 258
  // (the maximum string length) and number of input bytes available
  // at least ten.  The ten bytes are six bytes for the longest length/
  // distance pair plus four bytes for overloading the bit buffer.

InfCodes.prototype.inflate_fast = function(bl, bd, tl, tl_index, td, td_index, s, z) {
    var t;                // temporary pointer
    var   tp;             // temporary pointer (int[])
    var tp_index;         // temporary pointer
    var e;                // extra bits or operation
    var b;                // bit buffer
    var k;                // bits in bit buffer
    var p;                // input data pointer
    var n;                // bytes available there
    var q;                // output window write pointer
    var m;                // bytes to end of window or read pointer
    var ml;               // mask for literal/length tree
    var md;               // mask for distance tree
    var c;                // bytes to copy
    var d;                // distance back to copy from
    var r;                // copy source pointer

    var tp_index_t_3;     // (tp_index+t)*3

    // load input, output, bit values
    p=z.next_in_index;n=z.avail_in;b=s.bitb;k=s.bitk;
    q=s.write;m=q<s.read?s.read-q-1:s.end-q;

    // initialize masks
    ml = inflate_mask[bl];
    md = inflate_mask[bd];

    // do until not enough input or output space for fast loop
    do {                          // assume called with m >= 258 && n >= 10
      // get literal/length code
      while(k<(20)){              // max bits for literal/length code
	n--;
	b|=(z.next_in[p++]&0xff)<<k;k+=8;
      }

      t= b&ml;
      tp=tl; 
      tp_index=tl_index;
      tp_index_t_3=(tp_index+t)*3;
      if ((e = tp[tp_index_t_3]) == 0){
	b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	s.window[q++] = tp[tp_index_t_3+2];
	m--;
	continue;
      }
      do {

	b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	if((e&16)!=0){
	  e &= 15;
	  c = tp[tp_index_t_3+2] + (b & inflate_mask[e]);

	  b>>=e; k-=e;

	  // decode distance base of block to copy
	  while(k<(15)){           // max bits for distance code
	    n--;
	    b|=(z.next_in[p++]&0xff)<<k;k+=8;
	  }

	  t= b&md;
	  tp=td;
	  tp_index=td_index;
          tp_index_t_3=(tp_index+t)*3;
	  e = tp[tp_index_t_3];

	  do {

	    b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	    if((e&16)!=0){
	      // get extra bits to add to distance base
	      e &= 15;
	      while(k<(e)){         // get extra bits (up to 13)
		n--;
		b|=(z.next_in[p++]&0xff)<<k;k+=8;
	      }

	      d = tp[tp_index_t_3+2] + (b&inflate_mask[e]);

	      b>>=(e); k-=(e);

	      // do the copy
	      m -= c;
	      if (q >= d){                // offset before dest
		//  just copy
		r=q-d;
		if(q-r>0 && 2>(q-r)){           
		  s.window[q++]=s.window[r++]; // minimum count is three,
		  s.window[q++]=s.window[r++]; // so unroll loop a little
		  c-=2;
		}
		else{
		  s.window[q++]=s.window[r++]; // minimum count is three,
		  s.window[q++]=s.window[r++]; // so unroll loop a little
		  c-=2;
		}
	      }
	      else{                  // else offset after destination
                r=q-d;
                do{
                  r+=s.end;          // force pointer in window
                }while(r<0);         // covers invalid distances
		e=s.end-r;
		if(c>e){             // if source crosses,
		  c-=e;              // wrapped copy
		  if(q-r>0 && e>(q-r)){           
		    do{s.window[q++] = s.window[r++];}
		    while(--e!=0);
		  }
		  else{
		    arrayCopy(s.window, r, s.window, q, e);
		    q+=e; r+=e; e=0;
		  }
		  r = 0;                  // copy rest from start of window
		}

	      }

	      // copy all or what's left
              do{s.window[q++] = s.window[r++];}
		while(--c!=0);
	      break;
	    }
	    else if((e&64)==0){
	      t+=tp[tp_index_t_3+2];
	      t+=(b&inflate_mask[e]);
	      tp_index_t_3=(tp_index+t)*3;
	      e=tp[tp_index_t_3];
	    }
	    else{
	      z.msg = "invalid distance code";

	      c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

	      s.bitb=b;s.bitk=k;
	      z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	      s.write=q;

	      return Z_DATA_ERROR;
	    }
	  }
	  while(true);
	  break;
	}

	if((e&64)==0){
	  t+=tp[tp_index_t_3+2];
	  t+=(b&inflate_mask[e]);
	  tp_index_t_3=(tp_index+t)*3;
	  if((e=tp[tp_index_t_3])==0){

	    b>>=(tp[tp_index_t_3+1]); k-=(tp[tp_index_t_3+1]);

	    s.window[q++]=tp[tp_index_t_3+2];
	    m--;
	    break;
	  }
	}
	else if((e&32)!=0){

	  c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;
 
	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;

	  return Z_STREAM_END;
	}
	else{
	  z.msg="invalid literal/length code";

	  c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

	  s.bitb=b;s.bitk=k;
	  z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
	  s.write=q;

	  return Z_DATA_ERROR;
	}
      } 
      while(true);
    } 
    while(m>=258 && n>= 10);

    // not enough input or output--restore pointers and return
    c=z.avail_in-n;c=(k>>3)<c?k>>3:c;n+=c;p-=c;k-=c<<3;

    s.bitb=b;s.bitk=k;
    z.avail_in=n;z.total_in+=p-z.next_in_index;z.next_in_index=p;
    s.write=q;

    return Z_OK;
}

//
// InfTree.java
//

function InfTree() {
}

InfTree.prototype.huft_build = function(b, bindex, n, s, d, e, t, m, hp, hn, v) {

    // Given a list of code lengths and a maximum table size, make a set of
    // tables to decode that set of codes.  Return Z_OK on success, Z_BUF_ERROR
    // if the given code set is incomplete (the tables are still built in this
    // case), Z_DATA_ERROR if the input is invalid (an over-subscribed set of
    // lengths), or Z_MEM_ERROR if not enough memory.

    var a;                       // counter for codes of length k
    var f;                       // i repeats in table every f entries
    var g;                       // maximum code length
    var h;                       // table level
    var i;                       // counter, current code
    var j;                       // counter
    var k;                       // number of bits in current code
    var l;                       // bits per table (returned in m)
    var mask;                    // (1 << w) - 1, to avoid cc -O bug on HP
    var p;                       // pointer into c[], b[], or v[]
    var q;                       // points to current table
    var w;                       // bits before this table == (l * h)
    var xp;                      // pointer into x
    var y;                       // number of dummy codes added
    var z;                       // number of entries in current table

    // Generate counts for each bit length

    p = 0; i = n;
    do {
      this.c[b[bindex+p]]++; p++; i--;   // assume all entries <= BMAX
    }while(i!=0);

    if(this.c[0] == n){                // null input--all zero length codes
      t[0] = -1;
      m[0] = 0;
      return Z_OK;
    }

    // Find minimum and maximum length, bound *m by those
    l = m[0];
    for (j = 1; j <= BMAX; j++)
      if(this.c[j]!=0) break;
    k = j;                        // minimum code length
    if(l < j){
      l = j;
    }
    for (i = BMAX; i!=0; i--){
      if(this.c[i]!=0) break;
    }
    g = i;                        // maximum code length
    if(l > i){
      l = i;
    }
    m[0] = l;

    // Adjust last length count to fill out codes, if needed
    for (y = 1 << j; j < i; j++, y <<= 1){
      if ((y -= this.c[j]) < 0){
        return Z_DATA_ERROR;
      }
    }
    if ((y -= this.c[i]) < 0){
      return Z_DATA_ERROR;
    }
    this.c[i] += y;

    // Generate starting offsets into the value table for each length
    this.x[1] = j = 0;
    p = 1;  xp = 2;
    while (--i!=0) {                 // note that i == g from above
      this.x[xp] = (j += this.c[p]);
      xp++;
      p++;
    }

    // Make a table of values in order of bit lengths
    i = 0; p = 0;
    do {
      if ((j = b[bindex+p]) != 0){
        this.v[this.x[j]++] = i;
      }
      p++;
    }
    while (++i < n);
    n = this.x[g];                     // set n to length of v

    // Generate the Huffman codes and for each, make the table entries
    this.x[0] = i = 0;                 // first Huffman code is zero
    p = 0;                        // grab values in bit order
    h = -1;                       // no tables yet--level -1
    w = -l;                       // bits decoded == (l * h)
    this.u[0] = 0;                     // just to keep compilers happy
    q = 0;                        // ditto
    z = 0;                        // ditto

    // go through the bit lengths (k already is bits in shortest code)
    for (; k <= g; k++){
      a = this.c[k];
      while (a--!=0){
	// here i is the Huffman code of length k bits for value *p
	// make tables up to required level
        while (k > w + l){
          h++;
          w += l;                 // previous table always l bits
	  // compute minimum size table less than or equal to l bits
          z = g - w;
          z = (z > l) ? l : z;        // table size upper limit
          if((f=1<<(j=k-w))>a+1){     // try a k-w bit table
                                      // too few codes for k-w bit table
            f -= a + 1;               // deduct codes from patterns left
            xp = k;
            if(j < z){
              while (++j < z){        // try smaller tables up to z bits
                if((f <<= 1) <= this.c[++xp])
                  break;              // enough codes to use up j bits
                f -= this.c[xp];           // else deduct codes from patterns
              }
	    }
          }
          z = 1 << j;                 // table entries for j-bit table

	  // allocate new table
          if (this.hn[0] + z > MANY){       // (note: doesn't matter for fixed)
            return Z_DATA_ERROR;       // overflow of MANY
          }
          this.u[h] = q = /*hp+*/ this.hn[0];   // DEBUG
          this.hn[0] += z;
 
	  // connect to last table, if there is one
	  if(h!=0){
            this.x[h]=i;           // save pattern for backing up
            this.r[0]=j;     // bits in this table
            this.r[1]=l;     // bits to dump before this table
            j=i>>>(w - l);
            this.r[2] = (q - this.u[h-1] - j);               // offset to this table
            arrayCopy(this.r, 0, hp, (this.u[h-1]+j)*3, 3); // connect to last table
          }
          else{
            t[0] = q;               // first table is returned result
	  }
        }

	// set up table entry in r
        this.r[1] = (k - w);
        if (p >= n){
          this.r[0] = 128 + 64;      // out of values--invalid code
	}
        else if (v[p] < s){
          this.r[0] = (this.v[p] < 256 ? 0 : 32 + 64);  // 256 is end-of-block
          this.r[2] = this.v[p++];          // simple code is just the value
        }
        else{
          this.r[0]=(e[this.v[p]-s]+16+64); // non-simple--look up in lists
          this.r[2]=d[this.v[p++] - s];
        }

        // fill code-like entries with r
        f=1<<(k-w);
        for (j=i>>>w;j<z;j+=f){
          arrayCopy(this.r, 0, hp, (q+j)*3, 3);
	}

	// backwards increment the k-bit code i
        for (j = 1 << (k - 1); (i & j)!=0; j >>>= 1){
          i ^= j;
	}
        i ^= j;

	// backup over finished tables
        mask = (1 << w) - 1;      // needed on HP, cc -O bug
        while ((i & mask) != this.x[h]){
          h--;                    // don't need to update q
          w -= l;
          mask = (1 << w) - 1;
        }
      }
    }
    // Return Z_BUF_ERROR if we were given an incomplete table
    return y != 0 && g != 1 ? Z_BUF_ERROR : Z_OK;
}

InfTree.prototype.inflate_trees_bits = function(c, bb, tb, hp, z) {
    var result;
    this.initWorkArea(19);
    this.hn[0]=0;
    result = this.huft_build(c, 0, 19, 19, null, null, tb, bb, hp, this.hn, this.v);

    if(result == Z_DATA_ERROR){
      z.msg = "oversubscribed dynamic bit lengths tree";
    }
    else if(result == Z_BUF_ERROR || bb[0] == 0){
      z.msg = "incomplete dynamic bit lengths tree";
      result = Z_DATA_ERROR;
    }
    return result;
}

InfTree.prototype.inflate_trees_dynamic = function(nl, nd, c, bl, bd, tl, td, hp, z) {
    var result;

    // build literal/length tree
    this.initWorkArea(288);
    this.hn[0]=0;
    result = this.huft_build(c, 0, nl, 257, cplens, cplext, tl, bl, hp, this.hn, this.v);
    if (result != Z_OK || bl[0] == 0){
      if(result == Z_DATA_ERROR){
        z.msg = "oversubscribed literal/length tree";
      }
      else if (result != Z_MEM_ERROR){
        z.msg = "incomplete literal/length tree";
        result = Z_DATA_ERROR;
      }
      return result;
    }

    // build distance tree
    this.initWorkArea(288);
    result = this.huft_build(c, nl, nd, 0, cpdist, cpdext, td, bd, hp, this.hn, this.v);

    if (result != Z_OK || (bd[0] == 0 && nl > 257)){
      if (result == Z_DATA_ERROR){
        z.msg = "oversubscribed distance tree";
      }
      else if (result == Z_BUF_ERROR) {
        z.msg = "incomplete distance tree";
        result = Z_DATA_ERROR;
      }
      else if (result != Z_MEM_ERROR){
        z.msg = "empty distance tree with lengths";
        result = Z_DATA_ERROR;
      }
      return result;
    }

    return Z_OK;
}
/*
  static int inflate_trees_fixed(int[] bl,  //literal desired/actual bit depth
                                 int[] bd,  //distance desired/actual bit depth
                                 int[][] tl,//literal/length tree result
                                 int[][] td,//distance tree result 
                                 ZStream z  //for memory allocation
				 ){

*/

function inflate_trees_fixed(bl, bd, tl, td, z) {
    bl[0]=fixed_bl;
    bd[0]=fixed_bd;
    tl[0]=fixed_tl;
    td[0]=fixed_td;
    return Z_OK;
}

InfTree.prototype.initWorkArea = function(vsize){
    if(this.hn==null){
        this.hn=new Int32Array(1);
        this.v=new Int32Array(vsize);
        this.c=new Int32Array(BMAX+1);
        this.r=new Int32Array(3);
        this.u=new Int32Array(BMAX);
        this.x=new Int32Array(BMAX+1);
    }
    if(this.v.length<vsize){ 
        this.v=new Int32Array(vsize); 
    }
    for(var i=0; i<vsize; i++){this.v[i]=0;}
    for(var i=0; i<BMAX+1; i++){this.c[i]=0;}
    for(var i=0; i<3; i++){this.r[i]=0;}
//  for(int i=0; i<BMAX; i++){u[i]=0;}
    arrayCopy(this.c, 0, this.u, 0, BMAX);
//  for(int i=0; i<BMAX+1; i++){x[i]=0;}
    arrayCopy(this.c, 0, this.x, 0, BMAX+1);
}

var testArray = new Uint8Array(1);
var hasSubarray = (typeof testArray.subarray === 'function');
var hasSlice = false; /* (typeof testArray.slice === 'function'); */ // Chrome slice performance is so dire that we're currently not using it...

function arrayCopy(src, srcOffset, dest, destOffset, count) {
    if (count == 0) {
        return;
    } 
    if (!src) {
        throw "Undef src";
    } else if (!dest) {
        throw "Undef dest";
    }

    if (srcOffset == 0 && count == src.length) {
        arrayCopy_fast(src, dest, destOffset);
    } else if (hasSubarray) {
        arrayCopy_fast(src.subarray(srcOffset, srcOffset + count), dest, destOffset); 
    } else if (src.BYTES_PER_ELEMENT == 1 && count > 100) {
        arrayCopy_fast(new Uint8Array(src.buffer, src.byteOffset + srcOffset, count), dest, destOffset);
    } else { 
        arrayCopy_slow(src, srcOffset, dest, destOffset, count);
    }

}

function arrayCopy_slow(src, srcOffset, dest, destOffset, count) {

    // dlog('_slow call: srcOffset=' + srcOffset + '; destOffset=' + destOffset + '; count=' + count);

     for (var i = 0; i < count; ++i) {
        dest[destOffset + i] = src[srcOffset + i];
    }
}

function arrayCopy_fast(src, dest, destOffset) {
    dest.set(src, destOffset);
}


  // largest prime smaller than 65536
var ADLER_BASE=65521; 
  // NMAX is the largest n such that 255n(n+1)/2 + (n+1)(BASE-1) <= 2^32-1
var ADLER_NMAX=5552;

function adler32(adler, /* byte[] */ buf,  index, len){
    if(buf == null){ return 1; }

    var s1=adler&0xffff;
    var s2=(adler>>16)&0xffff;
    var k;

    while(len > 0) {
      k=len<ADLER_NMAX?len:ADLER_NMAX;
      len-=k;
      while(k>=16){
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        s1+=buf[index++]&0xff; s2+=s1;
        k-=16;
      }
      if(k!=0){
        do{
          s1+=buf[index++]&0xff; s2+=s1;
        }
        while(--k!=0);
      }
      s1%=ADLER_BASE;
      s2%=ADLER_BASE;
    }
    return (s2<<16)|s1;
}



function jszlib_inflate_buffer(buffer, start, length, afterUncOffset) {
    if (!start) {
        buffer = new Uint8Array(buffer);
    } else if (!length) {
        buffer = new Uint8Array(buffer, start, buffer.byteLength - start);
    } else {
        buffer = new Uint8Array(buffer, start, length);
    }

    var z = new ZStream();
    z.inflateInit(DEF_WBITS, true);
    z.next_in = buffer;
    z.next_in_index = 0;
    z.avail_in = buffer.length;

    var oBlockList = [];
    var totalSize = 0;
    while (true) {
        var obuf = new Uint8Array(32000);
        z.next_out = obuf;
        z.next_out_index = 0;
        z.avail_out = obuf.length;
        var status = z.inflate(Z_NO_FLUSH);
        if (status != Z_OK && status != Z_STREAM_END && status != Z_BUF_ERROR) {
            throw z.msg;
        }
        if (z.avail_out != 0) {
            var newob = new Uint8Array(obuf.length - z.avail_out);
            arrayCopy(obuf, 0, newob, 0, (obuf.length - z.avail_out));
            obuf = newob;
        }
        oBlockList.push(obuf);
        totalSize += obuf.length;
        if (status == Z_STREAM_END || status == Z_BUF_ERROR) {
            break;
        }
    }

    if (afterUncOffset) {
        afterUncOffset[0] = (start || 0) + z.next_in_index;
    }

    if (oBlockList.length == 1) {
        return oBlockList[0].buffer;
    } else {
        var out = new Uint8Array(totalSize);
        var cursor = 0;
        for (var i = 0; i < oBlockList.length; ++i) {
            var b = oBlockList[i];
            arrayCopy(b, 0, out, cursor, b.length);
            cursor += b.length;
        }
        return out.buffer;
    }
}

if (typeof(module) !== 'undefined') {
  module.exports = {
    inflateBuffer: jszlib_inflate_buffer,
    arrayCopy: arrayCopy
  };
}

},{}]},{},[6])
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL25vZGVfbW9kdWxlcy9ndWxwLWJyb3dzZXJpZnkvbm9kZV9tb2R1bGVzL2Jyb3dzZXJpZnkvbm9kZV9tb2R1bGVzL2Jyb3dzZXItcGFjay9fcHJlbHVkZS5qcyIsIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2JhbS5qcyIsIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2JpZ3dpZy5qcyIsIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2Jpbi5qcyIsIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL2pzL2NvbG9yLmpzIiwiL1VzZXJzL3Brb2svcmVwb3MvdHJhbnNtYXJ0LTEuMi9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvZGFzLmpzIiwiL1VzZXJzL3Brb2svcmVwb3MvdHJhbnNtYXJ0LTEuMi9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvZmFrZV8yMTQxNTBiLmpzIiwiL1VzZXJzL3Brb2svcmVwb3MvdHJhbnNtYXJ0LTEuMi9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvbGgzdXRpbHMuanMiLCIvVXNlcnMvcGtvay9yZXBvcy90cmFuc21hcnQtMS4yL2RhbGxpYW5jZS1wbHVnaW4vd2ViLWFwcC9qcy9zaGExLmpzIiwiL1VzZXJzL3Brb2svcmVwb3MvdHJhbnNtYXJ0LTEuMi9kYWxsaWFuY2UtcGx1Z2luL3dlYi1hcHAvanMvc3BhbnMuanMiLCIvVXNlcnMvcGtvay9yZXBvcy90cmFuc21hcnQtMS4yL2RhbGxpYW5jZS1wbHVnaW4vd2ViLWFwcC9qcy91dGlscy5qcyIsIi9Vc2Vycy9wa29rL3JlcG9zL3RyYW5zbWFydC0xLjIvZGFsbGlhbmNlLXBsdWdpbi93ZWItYXBwL25vZGVfbW9kdWxlcy9qc3psaWIvanMvaW5mbGF0ZS5qcyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiQUFBQTtBQ0FBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQ3BnQkE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDamtDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUMzUUE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUM1SEE7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDeDBCQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQzlMQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTs7QUM1R0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDblZBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7O0FDek5BO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBOztBQzdkQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0E7QUFDQTtBQUNBO0FBQ0EiLCJmaWxlIjoiZ2VuZXJhdGVkLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXNDb250ZW50IjpbIihmdW5jdGlvbiBlKHQsbixyKXtmdW5jdGlvbiBzKG8sdSl7aWYoIW5bb10pe2lmKCF0W29dKXt2YXIgYT10eXBlb2YgcmVxdWlyZT09XCJmdW5jdGlvblwiJiZyZXF1aXJlO2lmKCF1JiZhKXJldHVybiBhKG8sITApO2lmKGkpcmV0dXJuIGkobywhMCk7dGhyb3cgbmV3IEVycm9yKFwiQ2Fubm90IGZpbmQgbW9kdWxlICdcIitvK1wiJ1wiKX12YXIgZj1uW29dPXtleHBvcnRzOnt9fTt0W29dWzBdLmNhbGwoZi5leHBvcnRzLGZ1bmN0aW9uKGUpe3ZhciBuPXRbb11bMV1bZV07cmV0dXJuIHMobj9uOmUpfSxmLGYuZXhwb3J0cyxlLHQsbixyKX1yZXR1cm4gbltvXS5leHBvcnRzfXZhciBpPXR5cGVvZiByZXF1aXJlPT1cImZ1bmN0aW9uXCImJnJlcXVpcmU7Zm9yKHZhciBvPTA7bzxyLmxlbmd0aDtvKyspcyhyW29dKTtyZXR1cm4gc30pIiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gRGFsbGlhbmNlIEdlbm9tZSBFeHBsb3JlclxuLy8gKGMpIFRob21hcyBEb3duIDIwMDYtMjAxMVxuLy9cbi8vIGJhbS5qczogaW5kZXhlZCBiaW5hcnkgYWxpZ25tZW50c1xuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIHNwYW5zID0gcmVxdWlyZSgnLi9zcGFucycpO1xuICAgIHZhciBSYW5nZSA9IHNwYW5zLlJhbmdlO1xuICAgIHZhciB1bmlvbiA9IHNwYW5zLnVuaW9uO1xuICAgIHZhciBpbnRlcnNlY3Rpb24gPSBzcGFucy5pbnRlcnNlY3Rpb247XG5cbiAgICB2YXIgYmluID0gcmVxdWlyZSgnLi9iaW4nKTtcbiAgICB2YXIgcmVhZEludCA9IGJpbi5yZWFkSW50O1xuICAgIHZhciByZWFkU2hvcnQgPSBiaW4ucmVhZFNob3J0O1xuICAgIHZhciByZWFkQnl0ZSA9IGJpbi5yZWFkQnl0ZTtcbiAgICB2YXIgcmVhZEludDY0ID0gYmluLnJlYWRJbnQ2NDtcbiAgICB2YXIgcmVhZEZsb2F0ID0gYmluLnJlYWRGbG9hdDtcblxuICAgIHZhciBsaDN1dGlscyA9IHJlcXVpcmUoJy4vbGgzdXRpbHMnKTtcbiAgICB2YXIgcmVhZFZvYiA9IGxoM3V0aWxzLnJlYWRWb2I7XG4gICAgdmFyIHVuYmd6ZiA9IGxoM3V0aWxzLnVuYmd6ZjtcbiAgICB2YXIgcmVnMmJpbnMgPSBsaDN1dGlscy5yZWcyYmlucztcbiAgICB2YXIgQ2h1bmsgPSBsaDN1dGlscy5DaHVuaztcbn1cblxuXG52YXIgQkFNX01BR0lDID0gMHgxNGQ0MTQyO1xudmFyIEJBSV9NQUdJQyA9IDB4MTQ5NDE0MjtcblxudmFyIEJhbUZsYWdzID0ge1xuICAgIE1VTFRJUExFX1NFR01FTlRTOiAgICAgICAweDEsXG4gICAgQUxMX1NFR01FTlRTX0FMSUdOOiAgICAgIDB4MixcbiAgICBTRUdNRU5UX1VOTUFQUEVEOiAgICAgICAgMHg0LFxuICAgIE5FWFRfU0VHTUVOVF9VTk1BUFBFRDogICAweDgsXG4gICAgUkVWRVJTRV9DT01QTEVNRU5UOiAgICAgIDB4MTAsXG4gICAgTkVYVF9SRVZFUlNFX0NPTVBMRU1FTlQ6IDB4MjAsXG4gICAgRklSU1RfU0VHTUVOVDogICAgICAgICAgIDB4NDAsXG4gICAgTEFTVF9TRUdNRU5UOiAgICAgICAgICAgIDB4ODAsXG4gICAgU0VDT05EQVJZX0FMSUdOTUVOVDogICAgIDB4MTAwLFxuICAgIFFDX0ZBSUw6ICAgICAgICAgICAgICAgICAweDIwMCxcbiAgICBEVVBMSUNBVEU6ICAgICAgICAgICAgICAgMHg0MDAsXG4gICAgU1VQUExFTUVOVEFSWTogICAgICAgICAgIDB4ODAwXG59O1xuXG5mdW5jdGlvbiBCYW1GaWxlKCkge1xufVxuXG5cbi8vIENhbGN1bGF0ZSB0aGUgbGVuZ3RoIChpbiBieXRlcykgb2YgdGhlIEJBSSByZWYgc3RhcnRpbmcgYXQgb2Zmc2V0LlxuLy8gUmV0dXJucyB7bmJpbiwgbGVuZ3RoLCBtaW5CbG9ja0luZGV4fVxuZnVuY3Rpb24gX2dldEJhaVJlZkxlbmd0aCh1bmNiYSwgb2Zmc2V0KSB7XG4gICAgdmFyIHAgPSBvZmZzZXQ7XG4gICAgdmFyIG5iaW4gPSByZWFkSW50KHVuY2JhLCBwKTsgcCArPSA0O1xuICAgIGZvciAodmFyIGIgPSAwOyBiIDwgbmJpbjsgKytiKSB7XG4gICAgICAgIHZhciBiaW4gPSByZWFkSW50KHVuY2JhLCBwKTtcbiAgICAgICAgdmFyIG5jaG5rID0gcmVhZEludCh1bmNiYSwgcCs0KTtcbiAgICAgICAgcCArPSA4ICsgKG5jaG5rICogMTYpO1xuICAgIH1cbiAgICB2YXIgbmludHYgPSByZWFkSW50KHVuY2JhLCBwKTsgcCArPSA0O1xuXG4gICAgdmFyIG1pbkJsb2NrSW5kZXggPSAxMDAwMDAwMDAwO1xuICAgIHZhciBxID0gcDtcbiAgICBmb3IgKHZhciBpID0gMDsgaSA8IG5pbnR2OyArK2kpIHtcbiAgICAgICAgdmFyIHYgPSByZWFkVm9iKHVuY2JhLCBxKTsgcSArPSA4O1xuICAgICAgICBpZiAodikge1xuICAgICAgICAgICAgdmFyIGJpID0gdi5ibG9jaztcbiAgICAgICAgICAgIGlmICh2Lm9mZnNldCA+IDApXG4gICAgICAgICAgICAgICAgYmkgKz0gNjU1MzY7XG5cbiAgICAgICAgICAgIGlmIChiaSA8IG1pbkJsb2NrSW5kZXgpXG4gICAgICAgICAgICAgICAgbWluQmxvY2tJbmRleCA9IGJpO1xuICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcCArPSAobmludHYgKiA4KTtcblxuICAgIHJldHVybiB7XG4gICAgICAgIG1pbkJsb2NrSW5kZXg6IG1pbkJsb2NrSW5kZXgsXG4gICAgICAgIG5iaW46IG5iaW4sXG4gICAgICAgIGxlbmd0aDogcCAtIG9mZnNldFxuICAgIH07XG59XG5cblxuZnVuY3Rpb24gbWFrZUJhbShkYXRhLCBiYWksIGluZGV4Q2h1bmtzLCBjYWxsYmFjaykge1xuICAgIHZhciBiYW0gPSBuZXcgQmFtRmlsZSgpO1xuICAgIGJhbS5kYXRhID0gZGF0YTtcbiAgICBiYW0uYmFpID0gYmFpO1xuICAgIGJhbS5pbmRleENodW5rcyA9IGluZGV4Q2h1bmtzO1xuXG4gICAgdmFyIG1pbkJsb2NrSW5kZXggPSBiYW0uaW5kZXhDaHVua3MgPyBiYW0uaW5kZXhDaHVua3MubWluQmxvY2tJbmRleCA6IDEwMDAwMDAwMDA7XG5cbiAgICAvLyBGaWxscyBvdXQgYmFtLmNoclRvSW5kZXggYW5kIGJhbS5pbmRleFRvQ2hyIGJhc2VkIG9uIHRoZSBmaXJzdCBmZXcgYnl0ZXMgb2YgdGhlIEJBTS5cbiAgICBmdW5jdGlvbiBwYXJzZUJhbUhlYWRlcihyKSB7XG4gICAgICAgIGlmICghcikge1xuICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiQ291bGRuJ3QgYWNjZXNzIEJBTVwiKTtcbiAgICAgICAgfVxuXG4gICAgICAgIHZhciB1bmMgPSB1bmJnemYociwgci5ieXRlTGVuZ3RoKTtcbiAgICAgICAgdmFyIHVuY2JhID0gbmV3IFVpbnQ4QXJyYXkodW5jKTtcblxuICAgICAgICB2YXIgbWFnaWMgPSByZWFkSW50KHVuY2JhLCAwKTtcbiAgICAgICAgaWYgKG1hZ2ljICE9IEJBTV9NQUdJQykge1xuICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiTm90IGEgQkFNIGZpbGUsIG1hZ2ljPTB4XCIgKyBtYWdpYy50b1N0cmluZygxNikpO1xuICAgICAgICB9XG4gICAgICAgIHZhciBoZWFkTGVuID0gcmVhZEludCh1bmNiYSwgNCk7XG4gICAgICAgIHZhciBoZWFkZXIgPSAnJztcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBoZWFkTGVuOyArK2kpIHtcbiAgICAgICAgICAgIGhlYWRlciArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKHVuY2JhW2kgKyA4XSk7XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgblJlZiA9IHJlYWRJbnQodW5jYmEsIGhlYWRMZW4gKyA4KTtcbiAgICAgICAgdmFyIHAgPSBoZWFkTGVuICsgMTI7XG5cbiAgICAgICAgYmFtLmNoclRvSW5kZXggPSB7fTtcbiAgICAgICAgYmFtLmluZGV4VG9DaHIgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBuUmVmOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBsTmFtZSA9IHJlYWRJbnQodW5jYmEsIHApO1xuICAgICAgICAgICAgdmFyIG5hbWUgPSAnJztcbiAgICAgICAgICAgIGZvciAodmFyIGogPSAwOyBqIDwgbE5hbWUtMTsgKytqKSB7XG4gICAgICAgICAgICAgICAgbmFtZSArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKHVuY2JhW3AgKyA0ICsgal0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIGxSZWYgPSByZWFkSW50KHVuY2JhLCBwICsgbE5hbWUgKyA0KTtcbiAgICAgICAgICAgIGJhbS5jaHJUb0luZGV4W25hbWVdID0gaTtcbiAgICAgICAgICAgIGlmIChuYW1lLmluZGV4T2YoJ2NocicpID09IDApIHtcbiAgICAgICAgICAgICAgICBiYW0uY2hyVG9JbmRleFtuYW1lLnN1YnN0cmluZygzKV0gPSBpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBiYW0uY2hyVG9JbmRleFsnY2hyJyArIG5hbWVdID0gaTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGJhbS5pbmRleFRvQ2hyLnB1c2gobmFtZSk7XG5cbiAgICAgICAgICAgIHAgPSBwICsgOCArIGxOYW1lO1xuICAgICAgICB9XG5cbiAgICAgICAgaWYgKGJhbS5pbmRpY2VzKSB7XG4gICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soYmFtKTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIHBhcnNlQmFpKGhlYWRlcikge1xuICAgICAgICBpZiAoIWhlYWRlcikge1xuICAgICAgICAgICAgcmV0dXJuIFwiQ291bGRuJ3QgYWNjZXNzIEJBSVwiO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIHVuY2JhID0gbmV3IFVpbnQ4QXJyYXkoaGVhZGVyKTtcbiAgICAgICAgdmFyIGJhaU1hZ2ljID0gcmVhZEludCh1bmNiYSwgMCk7XG4gICAgICAgIGlmIChiYWlNYWdpYyAhPSBCQUlfTUFHSUMpIHtcbiAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsLCAnTm90IGEgQkFJIGZpbGUsIG1hZ2ljPTB4JyArIGJhaU1hZ2ljLnRvU3RyaW5nKDE2KSk7XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgbnJlZiA9IHJlYWRJbnQodW5jYmEsIDQpO1xuXG4gICAgICAgIGJhbS5pbmRpY2VzID0gW107XG5cbiAgICAgICAgdmFyIHAgPSA4O1xuICAgICAgICBmb3IgKHZhciByZWYgPSAwOyByZWYgPCBucmVmOyArK3JlZikge1xuICAgICAgICAgICAgdmFyIGJsb2NrU3RhcnQgPSBwO1xuICAgICAgICAgICAgdmFyIG8gPSBfZ2V0QmFpUmVmTGVuZ3RoKHVuY2JhLCBibG9ja1N0YXJ0KTtcbiAgICAgICAgICAgIHAgKz0gby5sZW5ndGg7XG5cbiAgICAgICAgICAgIG1pbkJsb2NrSW5kZXggPSBNYXRoLm1pbihvLm1pbkJsb2NrSW5kZXgsIG1pbkJsb2NrSW5kZXgpO1xuXG4gICAgICAgICAgICB2YXIgbmJpbiA9IG8ubmJpbjtcblxuICAgICAgICAgICAgaWYgKG5iaW4gPiAwKSB7XG4gICAgICAgICAgICAgICAgYmFtLmluZGljZXNbcmVmXSA9IG5ldyBVaW50OEFycmF5KGhlYWRlciwgYmxvY2tTdGFydCwgcCAtIGJsb2NrU3RhcnQpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgcmV0dXJuIHRydWU7XG4gICAgfVxuXG4gICAgaWYgKCFiYW0uaW5kZXhDaHVua3MpIHtcbiAgICAgICAgYmFtLmJhaS5mZXRjaChmdW5jdGlvbihoZWFkZXIpIHsgICAvLyBEbyB3ZSByZWFsbHkgbmVlZCB0byBmZXRjaCB0aGUgd2hvbGUgdGhpbmc/IDotKFxuICAgICAgICAgICAgdmFyIHJlc3VsdCA9IHBhcnNlQmFpKGhlYWRlcik7XG4gICAgICAgICAgICBpZiAocmVzdWx0ICE9PSB0cnVlKSB7XG4gICAgICAgICAgICAgIGNhbGxiYWNrKG51bGwsIHJlc3VsdCk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICBiYW0uZGF0YS5zbGljZSgwLCBtaW5CbG9ja0luZGV4KS5mZXRjaChwYXJzZUJhbUhlYWRlcik7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHZhciBjaHVua3MgPSBiYW0uaW5kZXhDaHVua3MuY2h1bmtzO1xuICAgICAgICBiYW0uaW5kaWNlcyA9IFtdXG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY2h1bmtzLmxlbmd0aDsgaSsrKSB7XG4gICAgICAgICAgIGJhbS5pbmRpY2VzW2ldID0gbnVsbDsgIC8vIFRvIGJlIGZpbGxlZCBvdXQgbGF6aWx5IGFzIG5lZWRlZFxuICAgICAgICB9XG4gICAgICAgIGJhbS5kYXRhLnNsaWNlKDAsIG1pbkJsb2NrSW5kZXgpLmZldGNoKHBhcnNlQmFtSGVhZGVyKTtcbiAgICB9XG59XG5cblxuXG5CYW1GaWxlLnByb3RvdHlwZS5ibG9ja3NGb3JSYW5nZSA9IGZ1bmN0aW9uKHJlZklkLCBtaW4sIG1heCkge1xuICAgIHZhciBpbmRleCA9IHRoaXMuaW5kaWNlc1tyZWZJZF07XG4gICAgaWYgKCFpbmRleCkge1xuICAgICAgICByZXR1cm4gW107XG4gICAgfVxuXG4gICAgdmFyIGludEJpbnNMID0gcmVnMmJpbnMobWluLCBtYXgpO1xuICAgIHZhciBpbnRCaW5zID0gW107XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBpbnRCaW5zTC5sZW5ndGg7ICsraSkge1xuICAgICAgICBpbnRCaW5zW2ludEJpbnNMW2ldXSA9IHRydWU7XG4gICAgfVxuICAgIHZhciBsZWFmQ2h1bmtzID0gW10sIG90aGVyQ2h1bmtzID0gW107XG5cbiAgICB2YXIgbmJpbiA9IHJlYWRJbnQoaW5kZXgsIDApO1xuICAgIHZhciBwID0gNDtcbiAgICBmb3IgKHZhciBiID0gMDsgYiA8IG5iaW47ICsrYikge1xuICAgICAgICB2YXIgYmluID0gcmVhZEludChpbmRleCwgcCk7XG4gICAgICAgIHZhciBuY2huayA9IHJlYWRJbnQoaW5kZXgsIHArNCk7XG4vLyAgICAgICAgZGxvZygnYmluPScgKyBiaW4gKyAnOyBuY2huaz0nICsgbmNobmspO1xuICAgICAgICBwICs9IDg7XG4gICAgICAgIGlmIChpbnRCaW5zW2Jpbl0pIHtcbiAgICAgICAgICAgIGZvciAodmFyIGMgPSAwOyBjIDwgbmNobms7ICsrYykge1xuICAgICAgICAgICAgICAgIHZhciBjcyA9IHJlYWRWb2IoaW5kZXgsIHApO1xuICAgICAgICAgICAgICAgIHZhciBjZSA9IHJlYWRWb2IoaW5kZXgsIHAgKyA4KTtcbiAgICAgICAgICAgICAgICAoYmluIDwgNDY4MSA/IG90aGVyQ2h1bmtzIDogbGVhZkNodW5rcykucHVzaChuZXcgQ2h1bmsoY3MsIGNlKSk7XG4gICAgICAgICAgICAgICAgcCArPSAxNjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHAgKz0gIChuY2huayAqIDE2KTtcbiAgICAgICAgfVxuICAgIH1cbiAgICAvLyBjb25zb2xlLmxvZygnbGVhZkNodW5rcyA9ICcgKyBtaW5pSlNPTmlmeShsZWFmQ2h1bmtzKSk7XG4gICAgLy8gY29uc29sZS5sb2coJ290aGVyQ2h1bmtzID0gJyArIG1pbmlKU09OaWZ5KG90aGVyQ2h1bmtzKSk7XG5cbiAgICB2YXIgbmludHYgPSByZWFkSW50KGluZGV4LCBwKTtcbiAgICB2YXIgbG93ZXN0ID0gbnVsbDtcbiAgICB2YXIgbWluTGluID0gTWF0aC5taW4obWluPj4xNCwgbmludHYgLSAxKSwgbWF4TGluID0gTWF0aC5taW4obWF4Pj4xNCwgbmludHYgLSAxKTtcbiAgICBmb3IgKHZhciBpID0gbWluTGluOyBpIDw9IG1heExpbjsgKytpKSB7XG4gICAgICAgIHZhciBsYiA9ICByZWFkVm9iKGluZGV4LCBwICsgNCArIChpICogOCkpO1xuICAgICAgICBpZiAoIWxiKSB7XG4gICAgICAgICAgICBjb250aW51ZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoIWxvd2VzdCB8fCBsYi5ibG9jayA8IGxvd2VzdC5ibG9jayB8fCBsYi5vZmZzZXQgPCBsb3dlc3Qub2Zmc2V0KSB7XG4gICAgICAgICAgICBsb3dlc3QgPSBsYjtcbiAgICAgICAgfVxuICAgIH1cbiAgICAvLyBjb25zb2xlLmxvZygnTG93ZXN0IExCID0gJyArIGxvd2VzdCk7XG4gICAgXG4gICAgdmFyIHBydW5lZE90aGVyQ2h1bmtzID0gW107XG4gICAgaWYgKGxvd2VzdCAhPSBudWxsKSB7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb3RoZXJDaHVua3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBjaG5rID0gb3RoZXJDaHVua3NbaV07XG4gICAgICAgICAgICBpZiAoY2huay5tYXh2LmJsb2NrID49IGxvd2VzdC5ibG9jayAmJiBjaG5rLm1heHYub2Zmc2V0ID49IGxvd2VzdC5vZmZzZXQpIHtcbiAgICAgICAgICAgICAgICBwcnVuZWRPdGhlckNodW5rcy5wdXNoKGNobmspO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIC8vIGNvbnNvbGUubG9nKCdwcnVuZWRPdGhlckNodW5rcyA9ICcgKyBtaW5pSlNPTmlmeShwcnVuZWRPdGhlckNodW5rcykpO1xuICAgIG90aGVyQ2h1bmtzID0gcHJ1bmVkT3RoZXJDaHVua3M7XG5cbiAgICB2YXIgaW50Q2h1bmtzID0gW107XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBvdGhlckNodW5rcy5sZW5ndGg7ICsraSkge1xuICAgICAgICBpbnRDaHVua3MucHVzaChvdGhlckNodW5rc1tpXSk7XG4gICAgfVxuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgbGVhZkNodW5rcy5sZW5ndGg7ICsraSkge1xuICAgICAgICBpbnRDaHVua3MucHVzaChsZWFmQ2h1bmtzW2ldKTtcbiAgICB9XG5cbiAgICBpbnRDaHVua3Muc29ydChmdW5jdGlvbihjMCwgYzEpIHtcbiAgICAgICAgdmFyIGRpZiA9IGMwLm1pbnYuYmxvY2sgLSBjMS5taW52LmJsb2NrO1xuICAgICAgICBpZiAoZGlmICE9IDApIHtcbiAgICAgICAgICAgIHJldHVybiBkaWY7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4gYzAubWludi5vZmZzZXQgLSBjMS5taW52Lm9mZnNldDtcbiAgICAgICAgfVxuICAgIH0pO1xuICAgIHZhciBtZXJnZWRDaHVua3MgPSBbXTtcbiAgICBpZiAoaW50Q2h1bmtzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgdmFyIGN1ciA9IGludENodW5rc1swXTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDE7IGkgPCBpbnRDaHVua3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBuYyA9IGludENodW5rc1tpXTtcbiAgICAgICAgICAgIGlmIChuYy5taW52LmJsb2NrID09IGN1ci5tYXh2LmJsb2NrIC8qICYmIG5jLm1pbnYub2Zmc2V0ID09IGN1ci5tYXh2Lm9mZnNldCAqLykgeyAvLyBubyBwb2ludCBzcGxpdHRpbmcgbWlkLWJsb2NrXG4gICAgICAgICAgICAgICAgY3VyID0gbmV3IENodW5rKGN1ci5taW52LCBuYy5tYXh2KTtcbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgbWVyZ2VkQ2h1bmtzLnB1c2goY3VyKTtcbiAgICAgICAgICAgICAgICBjdXIgPSBuYztcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBtZXJnZWRDaHVua3MucHVzaChjdXIpO1xuICAgIH1cbiAgICAvLyBkbG9nKCdtZXJnZWRDaHVua3MgPSAnICsgbWluaUpTT05pZnkobWVyZ2VkQ2h1bmtzKSk7XG5cbiAgICByZXR1cm4gbWVyZ2VkQ2h1bmtzO1xufVxuXG5CYW1GaWxlLnByb3RvdHlwZS5mZXRjaCA9IGZ1bmN0aW9uKGNociwgbWluLCBtYXgsIGNhbGxiYWNrLCBvcHRzKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICBvcHRzID0gb3B0cyB8fCB7fTtcblxuICAgIHZhciBjaHJJZCA9IHRoaXMuY2hyVG9JbmRleFtjaHJdO1xuICAgIHZhciBjaHVua3M7XG4gICAgaWYgKGNocklkID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgY2h1bmtzID0gW107XG4gICAgfSBlbHNlIHtcbiAgICAgICAgLy8gRmV0Y2ggdGhpcyBwb3J0aW9uIG9mIHRoZSBCQUkgaWYgaXQgaGFzbid0IGJlZW4gbG9hZGVkIHlldC5cbiAgICAgICAgaWYgKHRoaXMuaW5kaWNlc1tjaHJJZF0gPT09IG51bGwgJiYgdGhpcy5pbmRleENodW5rcy5jaHVua3NbY2hySWRdKSB7XG4gICAgICAgICAgICB2YXIgc3RhcnRfc3RvcCA9IHRoaXMuaW5kZXhDaHVua3MuY2h1bmtzW2NocklkXTtcbiAgICAgICAgICAgIHJldHVybiB0aGlzLmJhaS5zbGljZShzdGFydF9zdG9wWzBdLCBzdGFydF9zdG9wWzFdKS5mZXRjaChmdW5jdGlvbihkYXRhKSB7XG4gICAgICAgICAgICAgICAgdmFyIGJ1ZmZlciA9IG5ldyBVaW50OEFycmF5KGRhdGEpO1xuICAgICAgICAgICAgICAgIHRoaXMuaW5kaWNlc1tjaHJJZF0gPSBidWZmZXI7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHRoaXMuZmV0Y2goY2hyLCBtaW4sIG1heCwgY2FsbGJhY2ssIG9wdHMpO1xuICAgICAgICAgICAgfS5iaW5kKHRoaXMpKTtcbiAgICAgICAgfVxuXG4gICAgICAgIGNodW5rcyA9IHRoaXMuYmxvY2tzRm9yUmFuZ2UoY2hySWQsIG1pbiwgbWF4KTtcbiAgICAgICAgaWYgKCFjaHVua3MpIHtcbiAgICAgICAgICAgIGNhbGxiYWNrKG51bGwsICdFcnJvciBpbiBpbmRleCBmZXRjaCcpO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHZhciByZWNvcmRzID0gW107XG4gICAgdmFyIGluZGV4ID0gMDtcbiAgICB2YXIgZGF0YTtcblxuICAgIGZ1bmN0aW9uIHRyYW1wKCkge1xuICAgICAgICBpZiAoaW5kZXggPj0gY2h1bmtzLmxlbmd0aCkge1xuICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKHJlY29yZHMpO1xuICAgICAgICB9IGVsc2UgaWYgKCFkYXRhKSB7XG4gICAgICAgICAgICB2YXIgYyA9IGNodW5rc1tpbmRleF07XG4gICAgICAgICAgICB2YXIgZmV0Y2hNaW4gPSBjLm1pbnYuYmxvY2s7XG4gICAgICAgICAgICB2YXIgZmV0Y2hNYXggPSBjLm1heHYuYmxvY2sgKyAoMTw8MTYpOyAvLyAqc2lnaCpcbiAgICAgICAgICAgIHRoaXNCLmRhdGEuc2xpY2UoZmV0Y2hNaW4sIGZldGNoTWF4IC0gZmV0Y2hNaW4pLmZldGNoKGZ1bmN0aW9uKHIpIHtcbiAgICAgICAgICAgICAgICBkYXRhID0gdW5iZ3pmKHIsIGMubWF4di5ibG9jayAtIGMubWludi5ibG9jayArIDEpO1xuICAgICAgICAgICAgICAgIHJldHVybiB0cmFtcCgpO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShkYXRhKTtcbiAgICAgICAgICAgIHZhciBmaW5pc2hlZCA9IHRoaXNCLnJlYWRCYW1SZWNvcmRzKGJhLCBjaHVua3NbaW5kZXhdLm1pbnYub2Zmc2V0LCByZWNvcmRzLCBtaW4sIG1heCwgY2hySWQsIG9wdHMpO1xuICAgICAgICAgICAgZGF0YSA9IG51bGw7XG4gICAgICAgICAgICArK2luZGV4O1xuICAgICAgICAgICAgaWYgKGZpbmlzaGVkKVxuICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhyZWNvcmRzKTtcbiAgICAgICAgICAgIGVsc2VcbiAgICAgICAgICAgICAgICByZXR1cm4gdHJhbXAoKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICB0cmFtcCgpO1xufVxuXG52YXIgU0VRUkVUX0RFQ09ERVIgPSBbJz0nLCAnQScsICdDJywgJ3gnLCAnRycsICd4JywgJ3gnLCAneCcsICdUJywgJ3gnLCAneCcsICd4JywgJ3gnLCAneCcsICd4JywgJ04nXTtcbnZhciBDSUdBUl9ERUNPREVSID0gWydNJywgJ0knLCAnRCcsICdOJywgJ1MnLCAnSCcsICdQJywgJz0nLCAnWCcsICc/JywgJz8nLCAnPycsICc/JywgJz8nLCAnPycsICc/J107XG5cbmZ1bmN0aW9uIEJhbVJlY29yZCgpIHtcbn1cblxuQmFtRmlsZS5wcm90b3R5cGUucmVhZEJhbVJlY29yZHMgPSBmdW5jdGlvbihiYSwgb2Zmc2V0LCBzaW5rLCBtaW4sIG1heCwgY2hySWQsIG9wdHMpIHtcbiAgICB3aGlsZSAodHJ1ZSkge1xuICAgICAgICB2YXIgYmxvY2tTaXplID0gcmVhZEludChiYSwgb2Zmc2V0KTtcbiAgICAgICAgdmFyIGJsb2NrRW5kID0gb2Zmc2V0ICsgYmxvY2tTaXplICsgNDtcbiAgICAgICAgaWYgKGJsb2NrRW5kID49IGJhLmxlbmd0aCkge1xuICAgICAgICAgICAgcmV0dXJuIHNpbms7XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgcmVjb3JkID0gbmV3IEJhbVJlY29yZCgpO1xuXG4gICAgICAgIHZhciByZWZJRCA9IHJlYWRJbnQoYmEsIG9mZnNldCArIDQpO1xuICAgICAgICB2YXIgcG9zID0gcmVhZEludChiYSwgb2Zmc2V0ICsgOCk7XG4gICAgICAgIFxuICAgICAgICB2YXIgYm1uID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMTIpO1xuICAgICAgICB2YXIgYmluID0gKGJtbiAmIDB4ZmZmZjAwMDApID4+IDE2O1xuICAgICAgICB2YXIgbXEgPSAoYm1uICYgMHhmZjAwKSA+PiA4O1xuICAgICAgICB2YXIgbmwgPSBibW4gJiAweGZmO1xuXG4gICAgICAgIHZhciBmbGFnX25jID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMTYpO1xuICAgICAgICB2YXIgZmxhZyA9IChmbGFnX25jICYgMHhmZmZmMDAwMCkgPj4gMTY7XG4gICAgICAgIHZhciBuYyA9IGZsYWdfbmMgJiAweGZmZmY7XG4gICAgXG4gICAgICAgIHZhciBsc2VxID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMjApO1xuICAgICAgICBcbiAgICAgICAgdmFyIG5leHRSZWYgID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMjQpO1xuICAgICAgICB2YXIgbmV4dFBvcyA9IHJlYWRJbnQoYmEsIG9mZnNldCArIDI4KTtcbiAgICAgICAgXG4gICAgICAgIHZhciB0bGVuID0gcmVhZEludChiYSwgb2Zmc2V0ICsgMzIpO1xuICAgIFxuICAgICAgICByZWNvcmQuc2VnbWVudCA9IHRoaXMuaW5kZXhUb0NocltyZWZJRF07XG4gICAgICAgIHJlY29yZC5mbGFnID0gZmxhZztcbiAgICAgICAgcmVjb3JkLnBvcyA9IHBvcztcbiAgICAgICAgcmVjb3JkLm1xID0gbXE7XG4gICAgICAgIGlmIChvcHRzLmxpZ2h0KVxuICAgICAgICAgICAgcmVjb3JkLnNlcUxlbmd0aCA9IGxzZXE7XG5cbiAgICAgICAgaWYgKCFvcHRzLmxpZ2h0KSB7XG4gICAgICAgICAgICBpZiAobmV4dFJlZiA+PSAwKSB7XG4gICAgICAgICAgICAgICAgcmVjb3JkLm5leHRTZWdtZW50ID0gdGhpcy5pbmRleFRvQ2hyW25leHRSZWZdO1xuICAgICAgICAgICAgICAgIHJlY29yZC5uZXh0UG9zID0gbmV4dFBvcztcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgdmFyIHJlYWROYW1lID0gJyc7XG4gICAgICAgICAgICBmb3IgKHZhciBqID0gMDsgaiA8IG5sLTE7ICsraikge1xuICAgICAgICAgICAgICAgIHJlYWROYW1lICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbb2Zmc2V0ICsgMzYgKyBqXSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZWNvcmQucmVhZE5hbWUgPSByZWFkTmFtZTtcbiAgICAgICAgXG4gICAgICAgICAgICB2YXIgcCA9IG9mZnNldCArIDM2ICsgbmw7XG5cbiAgICAgICAgICAgIHZhciBjaWdhciA9ICcnO1xuICAgICAgICAgICAgZm9yICh2YXIgYyA9IDA7IGMgPCBuYzsgKytjKSB7XG4gICAgICAgICAgICAgICAgdmFyIGNpZ29wID0gcmVhZEludChiYSwgcCk7XG4gICAgICAgICAgICAgICAgY2lnYXIgPSBjaWdhciArIChjaWdvcD4+NCkgKyBDSUdBUl9ERUNPREVSW2NpZ29wICYgMHhmXTtcbiAgICAgICAgICAgICAgICBwICs9IDQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZWNvcmQuY2lnYXIgPSBjaWdhcjtcbiAgICAgICAgXG4gICAgICAgICAgICB2YXIgc2VxID0gJyc7XG4gICAgICAgICAgICB2YXIgc2VxQnl0ZXMgPSAobHNlcSArIDEpID4+IDE7XG4gICAgICAgICAgICBmb3IgKHZhciBqID0gMDsgaiA8IHNlcUJ5dGVzOyArK2opIHtcbiAgICAgICAgICAgICAgICB2YXIgc2IgPSBiYVtwICsgal07XG4gICAgICAgICAgICAgICAgc2VxICs9IFNFUVJFVF9ERUNPREVSWyhzYiAmIDB4ZjApID4+IDRdO1xuICAgICAgICAgICAgICAgIHNlcSArPSBTRVFSRVRfREVDT0RFUlsoc2IgJiAweDBmKV07XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBwICs9IHNlcUJ5dGVzO1xuICAgICAgICAgICAgcmVjb3JkLnNlcSA9IHNlcTtcblxuICAgICAgICAgICAgdmFyIHFzZXEgPSAnJztcbiAgICAgICAgICAgIGZvciAodmFyIGogPSAwOyBqIDwgbHNlcTsgKytqKSB7XG4gICAgICAgICAgICAgICAgcXNlcSArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGJhW3AgKyBqXSArIDMzKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHAgKz0gbHNlcTtcbiAgICAgICAgICAgIHJlY29yZC5xdWFscyA9IHFzZXE7XG5cbiAgICAgICAgICAgIHdoaWxlIChwIDwgYmxvY2tFbmQpIHtcbiAgICAgICAgICAgICAgICB2YXIgdGFnID0gU3RyaW5nLmZyb21DaGFyQ29kZShiYVtwXSwgYmFbcCArIDFdKTtcbiAgICAgICAgICAgICAgICB2YXIgdHlwZSA9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbcCArIDJdKTtcbiAgICAgICAgICAgICAgICB2YXIgdmFsdWU7XG5cbiAgICAgICAgICAgICAgICBpZiAodHlwZSA9PSAnQScpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWUgPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGJhW3AgKyAzXSk7XG4gICAgICAgICAgICAgICAgICAgIHAgKz0gNDtcbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKHR5cGUgPT0gJ2knIHx8IHR5cGUgPT0gJ0knKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhbHVlID0gcmVhZEludChiYSwgcCArIDMpO1xuICAgICAgICAgICAgICAgICAgICBwICs9IDc7XG4gICAgICAgICAgICAgICAgfSBlbHNlIGlmICh0eXBlID09ICdjJyB8fCB0eXBlID09ICdDJykge1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZSA9IGJhW3AgKyAzXTtcbiAgICAgICAgICAgICAgICAgICAgcCArPSA0O1xuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAodHlwZSA9PSAncycgfHwgdHlwZSA9PSAnUycpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWUgPSByZWFkU2hvcnQoYmEsIHAgKyAzKTtcbiAgICAgICAgICAgICAgICAgICAgcCArPSA1O1xuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAodHlwZSA9PSAnZicpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFsdWUgPSByZWFkRmxvYXQoYmEsIHAgKyAzKTtcbiAgICAgICAgICAgICAgICAgICAgcCArPSA3O1xuICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAodHlwZSA9PSAnWicgfHwgdHlwZSA9PSAnSCcpIHtcbiAgICAgICAgICAgICAgICAgICAgcCArPSAzO1xuICAgICAgICAgICAgICAgICAgICB2YWx1ZSA9ICcnO1xuICAgICAgICAgICAgICAgICAgICBmb3IgKDs7KSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY2MgPSBiYVtwKytdO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGNjID09IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFsdWUgKz0gU3RyaW5nLmZyb21DaGFyQ29kZShjYyk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKHR5cGUgPT0gJ0InKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBhdHlwZSA9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbcCArIDNdKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGFsZW4gPSByZWFkSW50KGJhLCBwICsgNCk7XG4gICAgICAgICAgICAgICAgICAgIHZhciBlbGVuO1xuICAgICAgICAgICAgICAgICAgICB2YXIgcmVhZGVyO1xuICAgICAgICAgICAgICAgICAgICBpZiAoYXR5cGUgPT0gJ2knIHx8IGF0eXBlID09ICdJJyB8fCBhdHlwZSA9PSAnZicpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIGVsZW4gPSA0O1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGF0eXBlID09ICdmJylcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZWFkZXIgPSByZWFkRmxvYXQ7XG4gICAgICAgICAgICAgICAgICAgICAgICBlbHNlXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcmVhZGVyID0gcmVhZEludDtcbiAgICAgICAgICAgICAgICAgICAgfSBlbHNlIGlmIChhdHlwZSA9PSAncycgfHwgYXR5cGUgPT0gJ1MnKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBlbGVuID0gMjtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlYWRlciA9IHJlYWRTaG9ydDtcbiAgICAgICAgICAgICAgICAgICAgfSBlbHNlIGlmIChhdHlwZSA9PSAnYycgfHwgYXR5cGUgPT0gJ0MnKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBlbGVuID0gMTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHJlYWRlciA9IHJlYWRCeXRlO1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhyb3cgJ1Vua25vd24gYXJyYXkgdHlwZSAnICsgYXR5cGU7XG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICBwICs9IDg7XG4gICAgICAgICAgICAgICAgICAgIHZhbHVlID0gW107XG4gICAgICAgICAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgYWxlbjsgKytpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZS5wdXNoKHJlYWRlcihiYSwgcCkpO1xuICAgICAgICAgICAgICAgICAgICAgICAgcCArPSBlbGVuO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgdGhyb3cgJ1Vua25vd24gdHlwZSAnKyB0eXBlO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICByZWNvcmRbdGFnXSA9IHZhbHVlO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKCFtaW4gfHwgcmVjb3JkLnBvcyA8PSBtYXggJiYgcmVjb3JkLnBvcyArIGxzZXEgPj0gbWluKSB7XG4gICAgICAgICAgICBpZiAoY2hySWQgPT09IHVuZGVmaW5lZCB8fCByZWZJRCA9PSBjaHJJZCkge1xuICAgICAgICAgICAgICAgIHNpbmsucHVzaChyZWNvcmQpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGlmIChyZWNvcmQucG9zID4gbWF4KSB7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBvZmZzZXQgPSBibG9ja0VuZDtcbiAgICB9XG5cbiAgICAvLyBFeGl0cyB2aWEgdG9wIG9mIGxvb3AuXG59O1xuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIG1ha2VCYW06IG1ha2VCYW0sXG4gICAgICAgIEJBTV9NQUdJQzogQkFNX01BR0lDLFxuICAgICAgICBCQUlfTUFHSUM6IEJBSV9NQUdJQyxcbiAgICAgICAgQmFtRmxhZ3M6IEJhbUZsYWdzXG4gICAgfTtcbn1cbiIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTBcbi8vXG4vLyBiaWd3aWcuanM6IGluZGV4ZWQgYmluYXJ5IFdJRyAoYW5kIEJFRCkgZmlsZXNcbi8vXG5cblwidXNlIHN0cmljdFwiO1xuXG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIHNwYW5zID0gcmVxdWlyZSgnLi9zcGFucycpO1xuICAgIHZhciBSYW5nZSA9IHNwYW5zLlJhbmdlO1xuICAgIHZhciB1bmlvbiA9IHNwYW5zLnVuaW9uO1xuICAgIHZhciBpbnRlcnNlY3Rpb24gPSBzcGFucy5pbnRlcnNlY3Rpb247XG5cbiAgICB2YXIgZGFzID0gcmVxdWlyZSgnLi9kYXMnKTtcbiAgICB2YXIgREFTRmVhdHVyZSA9IGRhcy5EQVNGZWF0dXJlO1xuICAgIHZhciBEQVNHcm91cCA9IGRhcy5EQVNHcm91cDtcblxuICAgIHZhciB1dGlscyA9IHJlcXVpcmUoJy4vdXRpbHMnKTtcbiAgICB2YXIgc2hhbGxvd0NvcHkgPSB1dGlscy5zaGFsbG93Q29weTtcblxuICAgIHZhciBiaW4gPSByZXF1aXJlKCcuL2JpbicpO1xuICAgIHZhciByZWFkSW50ID0gYmluLnJlYWRJbnQ7XG5cbiAgICB2YXIganN6bGliID0gcmVxdWlyZSgnanN6bGliJyk7XG4gICAgdmFyIGpzemxpYl9pbmZsYXRlX2J1ZmZlciA9IGpzemxpYi5pbmZsYXRlQnVmZmVyO1xuICAgIHZhciBhcnJheUNvcHkgPSBqc3psaWIuYXJyYXlDb3B5O1xufVxuXG52YXIgQklHX1dJR19NQUdJQyA9IDB4ODg4RkZDMjY7XG52YXIgQklHX1dJR19NQUdJQ19CRSA9IDB4MjZGQzhGODg7XG52YXIgQklHX0JFRF9NQUdJQyA9IDB4ODc4OUYyRUI7XG52YXIgQklHX0JFRF9NQUdJQ19CRSA9IDB4RUJGMjg5ODc7XG5cblxudmFyIEJJR19XSUdfVFlQRV9HUkFQSCA9IDE7XG52YXIgQklHX1dJR19UWVBFX1ZTVEVQID0gMjtcbnZhciBCSUdfV0lHX1RZUEVfRlNURVAgPSAzO1xuICBcbnZhciBNMSA9IDI1NjtcbnZhciBNMiA9IDI1NioyNTY7XG52YXIgTTMgPSAyNTYqMjU2KjI1NjtcbnZhciBNNCA9IDI1NioyNTYqMjU2KjI1NjtcblxudmFyIEJFRF9DT0xPUl9SRUdFWFAgPSBuZXcgUmVnRXhwKFwiXlswLTldKyxbMC05XSssWzAtOV0rXCIpO1xuXG5mdW5jdGlvbiBid2dfcmVhZE9mZnNldChiYSwgbykge1xuICAgIHZhciBvZmZzZXQgPSBiYVtvXSArIGJhW28rMV0qTTEgKyBiYVtvKzJdKk0yICsgYmFbbyszXSpNMyArIGJhW28rNF0qTTQ7XG4gICAgcmV0dXJuIG9mZnNldDtcbn1cblxuZnVuY3Rpb24gQmlnV2lnKCkge1xufVxuXG5CaWdXaWcucHJvdG90eXBlLnJlYWRDaHJvbVRyZWUgPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG4gICAgdGhpcy5jaHJvbXNUb0lEcyA9IHt9O1xuICAgIHRoaXMuaWRzVG9DaHJvbXMgPSB7fTtcbiAgICB0aGlzLm1heElEID0gMDtcblxuICAgIHZhciB1ZG8gPSB0aGlzLnVuem9vbWVkRGF0YU9mZnNldDtcbiAgICB2YXIgZWIgPSAodWRvIC0gdGhpcy5jaHJvbVRyZWVPZmZzZXQpICYgMztcbiAgICB1ZG8gPSB1ZG8gKyA0IC0gZWI7XG5cbiAgICB0aGlzLmRhdGEuc2xpY2UodGhpcy5jaHJvbVRyZWVPZmZzZXQsIHVkbyAtIHRoaXMuY2hyb21UcmVlT2Zmc2V0KS5mZXRjaChmdW5jdGlvbihicHQpIHtcbiAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoYnB0KTtcbiAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoYnB0KTtcbiAgICAgICAgdmFyIGxhID0gbmV3IEludDMyQXJyYXkoYnB0KTtcbiAgICAgICAgdmFyIGJwdE1hZ2ljID0gbGFbMF07XG4gICAgICAgIHZhciBibG9ja1NpemUgPSBsYVsxXTtcbiAgICAgICAgdmFyIGtleVNpemUgPSBsYVsyXTtcbiAgICAgICAgdmFyIHZhbFNpemUgPSBsYVszXTtcbiAgICAgICAgdmFyIGl0ZW1Db3VudCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCAxNik7XG4gICAgICAgIHZhciByb290Tm9kZU9mZnNldCA9IDMyO1xuXG4gICAgICAgIHZhciBicHRSZWFkTm9kZSA9IGZ1bmN0aW9uKG9mZnNldCkge1xuICAgICAgICAgICAgdmFyIG5vZGVUeXBlID0gYmFbb2Zmc2V0XTtcbiAgICAgICAgICAgIHZhciBjbnQgPSBzYVsob2Zmc2V0LzIpICsgMV07XG4gICAgICAgICAgICBvZmZzZXQgKz0gNDtcbiAgICAgICAgICAgIGZvciAodmFyIG4gPSAwOyBuIDwgY250OyArK24pIHtcbiAgICAgICAgICAgICAgICBpZiAobm9kZVR5cGUgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICBvZmZzZXQgKz0ga2V5U2l6ZTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGNoaWxkT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIG9mZnNldCk7XG4gICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSA4O1xuICAgICAgICAgICAgICAgICAgICBjaGlsZE9mZnNldCAtPSB0aGlzQi5jaHJvbVRyZWVPZmZzZXQ7XG4gICAgICAgICAgICAgICAgICAgIGJwdFJlYWROb2RlKGNoaWxkT2Zmc2V0KTtcbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICB2YXIga2V5ID0gJyc7XG4gICAgICAgICAgICAgICAgICAgIGZvciAodmFyIGtpID0gMDsga2kgPCBrZXlTaXplOyArK2tpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY2hhckNvZGUgPSBiYVtvZmZzZXQrK107XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoY2hhckNvZGUgIT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGtleSArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGNoYXJDb2RlKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB2YXIgY2hyb21JZCA9IChiYVtvZmZzZXQrM108PDI0KSB8IChiYVtvZmZzZXQrMl08PDE2KSB8IChiYVtvZmZzZXQrMV08PDgpIHwgKGJhW29mZnNldCswXSk7XG4gICAgICAgICAgICAgICAgICAgIHZhciBjaHJvbVNpemUgPSAoYmFbb2Zmc2V0ICsgN108PDI0KSB8IChiYVtvZmZzZXQrNl08PDE2KSB8IChiYVtvZmZzZXQrNV08PDgpIHwgKGJhW29mZnNldCs0XSk7XG4gICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSA4O1xuXG4gICAgICAgICAgICAgICAgICAgIHRoaXNCLmNocm9tc1RvSURzW2tleV0gPSBjaHJvbUlkO1xuICAgICAgICAgICAgICAgICAgICBpZiAoa2V5LmluZGV4T2YoJ2NocicpID09IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHRoaXNCLmNocm9tc1RvSURzW2tleS5zdWJzdHIoMyldID0gY2hyb21JZDtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB0aGlzQi5pZHNUb0Nocm9tc1tjaHJvbUlkXSA9IGtleTtcbiAgICAgICAgICAgICAgICAgICAgdGhpc0IubWF4SUQgPSBNYXRoLm1heCh0aGlzQi5tYXhJRCwgY2hyb21JZCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9O1xuICAgICAgICBicHRSZWFkTm9kZShyb290Tm9kZU9mZnNldCk7XG5cbiAgICAgICAgY2FsbGJhY2sodGhpc0IpO1xuICAgIH0pO1xufVxuXG5mdW5jdGlvbiBCaWdXaWdWaWV3KGJ3ZywgY2lyVHJlZU9mZnNldCwgY2lyVHJlZUxlbmd0aCwgaXNTdW1tYXJ5KSB7XG4gICAgdGhpcy5id2cgPSBid2c7XG4gICAgdGhpcy5jaXJUcmVlT2Zmc2V0ID0gY2lyVHJlZU9mZnNldDtcbiAgICB0aGlzLmNpclRyZWVMZW5ndGggPSBjaXJUcmVlTGVuZ3RoO1xuICAgIHRoaXMuaXNTdW1tYXJ5ID0gaXNTdW1tYXJ5O1xufVxuXG5cblxuQmlnV2lnVmlldy5wcm90b3R5cGUucmVhZFdpZ0RhdGEgPSBmdW5jdGlvbihjaHJOYW1lLCBtaW4sIG1heCwgY2FsbGJhY2spIHtcbiAgICB2YXIgY2hyID0gdGhpcy5id2cuY2hyb21zVG9JRHNbY2hyTmFtZV07XG4gICAgaWYgKGNociA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIC8vIE5vdCBhbiBlcnJvciBiZWNhdXNlIHNvbWUgLmJ3Z3Mgd29uJ3QgaGF2ZSBkYXRhIGZvciBhbGwgY2hyb21vc29tZXMuXG4gICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgdGhpcy5yZWFkV2lnRGF0YUJ5SWQoY2hyLCBtaW4sIG1heCwgY2FsbGJhY2spO1xuICAgIH1cbn1cblxuQmlnV2lnVmlldy5wcm90b3R5cGUucmVhZFdpZ0RhdGFCeUlkID0gZnVuY3Rpb24oY2hyLCBtaW4sIG1heCwgY2FsbGJhY2spIHtcbiAgICB2YXIgdGhpc0IgPSB0aGlzO1xuICAgIGlmICghdGhpcy5jaXJIZWFkZXIpIHtcbiAgICAgICAgdGhpcy5id2cuZGF0YS5zbGljZSh0aGlzLmNpclRyZWVPZmZzZXQsIDQ4KS5mZXRjaChmdW5jdGlvbihyZXN1bHQpIHtcbiAgICAgICAgICAgIHRoaXNCLmNpckhlYWRlciA9IHJlc3VsdDtcbiAgICAgICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KHRoaXNCLmNpckhlYWRlcik7XG4gICAgICAgICAgICB0aGlzQi5jaXJCbG9ja1NpemUgPSBsYVsxXTtcbiAgICAgICAgICAgIHRoaXNCLnJlYWRXaWdEYXRhQnlJZChjaHIsIG1pbiwgbWF4LCBjYWxsYmFjayk7XG4gICAgICAgIH0pO1xuICAgICAgICByZXR1cm47XG4gICAgfVxuXG4gICAgdmFyIGJsb2Nrc1RvRmV0Y2ggPSBbXTtcbiAgICB2YXIgb3V0c3RhbmRpbmcgPSAwO1xuXG4gICAgdmFyIGJlZm9yZUJXRyA9IERhdGUubm93KCk7XG5cbiAgICB2YXIgZmlsdGVyID0gZnVuY3Rpb24oY2hyb21JZCwgZm1pbiwgZm1heCwgdG9rcykge1xuICAgICAgICByZXR1cm4gKChjaHIgPCAwIHx8IGNocm9tSWQgPT0gY2hyKSAmJiBmbWluIDw9IG1heCAmJiBmbWF4ID49IG1pbik7XG4gICAgfVxuXG4gICAgdmFyIGNpckZvYlJlY3VyID0gZnVuY3Rpb24ob2Zmc2V0LCBsZXZlbCkge1xuICAgICAgICBpZiAodGhpc0IuYndnLmluc3RydW1lbnQpXG4gICAgICAgICAgICBjb25zb2xlLmxvZygnbGV2ZWw9JyArIGxldmVsICsgJzsgb2Zmc2V0PScgKyBvZmZzZXQgKyAnOyB0aW1lPScgKyAoRGF0ZS5ub3coKXwwKSk7XG5cbiAgICAgICAgb3V0c3RhbmRpbmcgKz0gb2Zmc2V0Lmxlbmd0aDtcblxuICAgICAgICBpZiAob2Zmc2V0Lmxlbmd0aCA9PSAxICYmIG9mZnNldFswXSAtIHRoaXNCLmNpclRyZWVPZmZzZXQgPT0gNDggJiYgdGhpc0IuY2FjaGVkQ2lyUm9vdCkge1xuICAgICAgICAgICAgY2lyRm9iUmVjdXIyKHRoaXNCLmNhY2hlZENpclJvb3QsIDAsIGxldmVsKTtcbiAgICAgICAgICAgIC0tb3V0c3RhbmRpbmc7XG4gICAgICAgICAgICBpZiAob3V0c3RhbmRpbmcgPT0gMCkge1xuICAgICAgICAgICAgICAgIHRoaXNCLmZldGNoRmVhdHVyZXMoZmlsdGVyLCBibG9ja3NUb0ZldGNoLCBjYWxsYmFjayk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgbWF4Q2lyQmxvY2tTcGFuID0gNCArICAodGhpc0IuY2lyQmxvY2tTaXplICogMzIpOyAgIC8vIFVwcGVyIGJvdW5kIG9uIHNpemUsIGJhc2VkIG9uIGEgY29tcGxldGVseSBmdWxsIGxlYWYgbm9kZS5cbiAgICAgICAgdmFyIHNwYW5zO1xuICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG9mZnNldC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGJsb2NrU3BhbiA9IG5ldyBSYW5nZShvZmZzZXRbaV0sIG9mZnNldFtpXSArIG1heENpckJsb2NrU3Bhbik7XG4gICAgICAgICAgICBzcGFucyA9IHNwYW5zID8gdW5pb24oc3BhbnMsIGJsb2NrU3BhbikgOiBibG9ja1NwYW47XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIHZhciBmZXRjaFJhbmdlcyA9IHNwYW5zLnJhbmdlcygpO1xuICAgICAgICBmb3IgKHZhciByID0gMDsgciA8IGZldGNoUmFuZ2VzLmxlbmd0aDsgKytyKSB7XG4gICAgICAgICAgICB2YXIgZnIgPSBmZXRjaFJhbmdlc1tyXTtcbiAgICAgICAgICAgIGNpckZvYlN0YXJ0RmV0Y2gob2Zmc2V0LCBmciwgbGV2ZWwpO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgdmFyIGNpckZvYlN0YXJ0RmV0Y2ggPSBmdW5jdGlvbihvZmZzZXQsIGZyLCBsZXZlbCwgYXR0ZW1wdHMpIHtcbiAgICAgICAgdmFyIGxlbmd0aCA9IGZyLm1heCgpIC0gZnIubWluKCk7XG4gICAgICAgIHRoaXNCLmJ3Zy5kYXRhLnNsaWNlKGZyLm1pbigpLCBmci5tYXgoKSAtIGZyLm1pbigpKS5mZXRjaChmdW5jdGlvbihyZXN1bHRCdWZmZXIpIHtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb2Zmc2V0Lmxlbmd0aDsgKytpKSB7XG4gICAgICAgICAgICAgICAgaWYgKGZyLmNvbnRhaW5zKG9mZnNldFtpXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgY2lyRm9iUmVjdXIyKHJlc3VsdEJ1ZmZlciwgb2Zmc2V0W2ldIC0gZnIubWluKCksIGxldmVsKTtcblxuICAgICAgICAgICAgICAgICAgICBpZiAob2Zmc2V0W2ldIC0gdGhpc0IuY2lyVHJlZU9mZnNldCA9PSA0OCAmJiBvZmZzZXRbaV0gLSBmci5taW4oKSA9PSAwKVxuICAgICAgICAgICAgICAgICAgICAgICAgdGhpc0IuY2FjaGVkQ2lyUm9vdCA9IHJlc3VsdEJ1ZmZlcjtcblxuICAgICAgICAgICAgICAgICAgICAtLW91dHN0YW5kaW5nO1xuICAgICAgICAgICAgICAgICAgICBpZiAob3V0c3RhbmRpbmcgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdGhpc0IuZmV0Y2hGZWF0dXJlcyhmaWx0ZXIsIGJsb2Nrc1RvRmV0Y2gsIGNhbGxiYWNrKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgdmFyIGNpckZvYlJlY3VyMiA9IGZ1bmN0aW9uKGNpckJsb2NrRGF0YSwgb2Zmc2V0LCBsZXZlbCkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShjaXJCbG9ja0RhdGEpO1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShjaXJCbG9ja0RhdGEpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShjaXJCbG9ja0RhdGEpO1xuXG4gICAgICAgIHZhciBpc0xlYWYgPSBiYVtvZmZzZXRdO1xuICAgICAgICB2YXIgY250ID0gc2Fbb2Zmc2V0LzIgKyAxXTtcbiAgICAgICAgb2Zmc2V0ICs9IDQ7XG5cbiAgICAgICAgaWYgKGlzTGVhZiAhPSAwKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGNudDsgKytpKSB7XG4gICAgICAgICAgICAgICAgdmFyIGxvID0gb2Zmc2V0LzQ7XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0Q2hyb20gPSBsYVtsb107XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0QmFzZSA9IGxhW2xvICsgMV07XG4gICAgICAgICAgICAgICAgdmFyIGVuZENocm9tID0gbGFbbG8gKyAyXTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kQmFzZSA9IGxhW2xvICsgM107XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIG9mZnNldCsxNik7XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrU2l6ZSA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBvZmZzZXQrMjQpO1xuICAgICAgICAgICAgICAgIGlmICgoKGNociA8IDAgfHwgc3RhcnRDaHJvbSA8IGNocikgfHwgKHN0YXJ0Q2hyb20gPT0gY2hyICYmIHN0YXJ0QmFzZSA8PSBtYXgpKSAmJlxuICAgICAgICAgICAgICAgICAgICAoKGNociA8IDAgfHwgZW5kQ2hyb20gICA+IGNocikgfHwgKGVuZENocm9tID09IGNociAmJiBlbmRCYXNlID49IG1pbikpKVxuICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgYmxvY2tzVG9GZXRjaC5wdXNoKHtvZmZzZXQ6IGJsb2NrT2Zmc2V0LCBzaXplOiBibG9ja1NpemV9KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgb2Zmc2V0ICs9IDMyO1xuICAgICAgICAgICAgfVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgdmFyIHJlY3VyT2Zmc2V0cyA9IFtdO1xuICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBjbnQ7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBsbyA9IG9mZnNldC80O1xuICAgICAgICAgICAgICAgIHZhciBzdGFydENocm9tID0gbGFbbG9dO1xuICAgICAgICAgICAgICAgIHZhciBzdGFydEJhc2UgPSBsYVtsbyArIDFdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRDaHJvbSA9IGxhW2xvICsgMl07XG4gICAgICAgICAgICAgICAgdmFyIGVuZEJhc2UgPSBsYVtsbyArIDNdO1xuICAgICAgICAgICAgICAgIHZhciBibG9ja09mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBvZmZzZXQrMTYpO1xuICAgICAgICAgICAgICAgIGlmICgoY2hyIDwgMCB8fCBzdGFydENocm9tIDwgY2hyIHx8IChzdGFydENocm9tID09IGNociAmJiBzdGFydEJhc2UgPD0gbWF4KSkgJiZcbiAgICAgICAgICAgICAgICAgICAgKGNociA8IDAgfHwgZW5kQ2hyb20gICA+IGNociB8fCAoZW5kQ2hyb20gPT0gY2hyICYmIGVuZEJhc2UgPj0gbWluKSkpXG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICByZWN1ck9mZnNldHMucHVzaChibG9ja09mZnNldCk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIG9mZnNldCArPSAyNDtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChyZWN1ck9mZnNldHMubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgIGNpckZvYlJlY3VyKHJlY3VyT2Zmc2V0cywgbGV2ZWwgKyAxKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH07XG5cbiAgICBjaXJGb2JSZWN1cihbdGhpc0IuY2lyVHJlZU9mZnNldCArIDQ4XSwgMSk7XG59XG5cblxuQmlnV2lnVmlldy5wcm90b3R5cGUuZmV0Y2hGZWF0dXJlcyA9IGZ1bmN0aW9uKGZpbHRlciwgYmxvY2tzVG9GZXRjaCwgY2FsbGJhY2spIHtcbiAgICB2YXIgdGhpc0IgPSB0aGlzO1xuXG4gICAgYmxvY2tzVG9GZXRjaC5zb3J0KGZ1bmN0aW9uKGIwLCBiMSkge1xuICAgICAgICByZXR1cm4gKGIwLm9mZnNldHwwKSAtIChiMS5vZmZzZXR8MCk7XG4gICAgfSk7XG5cbiAgICBpZiAoYmxvY2tzVG9GZXRjaC5sZW5ndGggPT0gMCkge1xuICAgICAgICBjYWxsYmFjayhbXSk7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgdmFyIGZlYXR1cmVzID0gW107XG4gICAgICAgIHZhciBjcmVhdGVGZWF0dXJlID0gZnVuY3Rpb24oY2hyLCBmbWluLCBmbWF4LCBvcHRzKSB7XG4gICAgICAgICAgICBpZiAoIW9wdHMpIHtcbiAgICAgICAgICAgICAgICBvcHRzID0ge307XG4gICAgICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAgICAgdmFyIGYgPSBuZXcgREFTRmVhdHVyZSgpO1xuICAgICAgICAgICAgZi5fY2hyb21JZCA9IGNocjtcbiAgICAgICAgICAgIGYuc2VnbWVudCA9IHRoaXNCLmJ3Zy5pZHNUb0Nocm9tc1tjaHJdO1xuICAgICAgICAgICAgZi5taW4gPSBmbWluO1xuICAgICAgICAgICAgZi5tYXggPSBmbWF4O1xuICAgICAgICAgICAgZi50eXBlID0gJ2JpZ3dpZyc7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGZvciAodmFyIGsgaW4gb3B0cykge1xuICAgICAgICAgICAgICAgIGZba10gPSBvcHRzW2tdO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgXG4gICAgICAgICAgICBmZWF0dXJlcy5wdXNoKGYpO1xuICAgICAgICB9O1xuXG4gICAgICAgIHZhciB0cmFtcCA9IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKGJsb2Nrc1RvRmV0Y2gubGVuZ3RoID09IDApIHtcbiAgICAgICAgICAgICAgICB2YXIgYWZ0ZXJCV0cgPSBEYXRlLm5vdygpO1xuICAgICAgICAgICAgICAgIC8vIGRsb2coJ0JXRyBmZXRjaCB0b29rICcgKyAoYWZ0ZXJCV0cgLSBiZWZvcmVCV0cpICsgJ21zJyk7XG4gICAgICAgICAgICAgICAgY2FsbGJhY2soZmVhdHVyZXMpO1xuICAgICAgICAgICAgICAgIHJldHVybjsgIC8vIGp1c3QgaW4gY2FzZS4uLlxuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICB2YXIgYmxvY2sgPSBibG9ja3NUb0ZldGNoWzBdO1xuICAgICAgICAgICAgICAgIGlmIChibG9jay5kYXRhKSB7XG4gICAgICAgICAgICAgICAgICAgIHRoaXNCLnBhcnNlRmVhdHVyZXMoYmxvY2suZGF0YSwgY3JlYXRlRmVhdHVyZSwgZmlsdGVyKTtcbiAgICAgICAgICAgICAgICAgICAgYmxvY2tzVG9GZXRjaC5zcGxpY2UoMCwgMSk7XG4gICAgICAgICAgICAgICAgICAgIHRyYW1wKCk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGZldGNoU3RhcnQgPSBibG9jay5vZmZzZXQ7XG4gICAgICAgICAgICAgICAgICAgIHZhciBmZXRjaFNpemUgPSBibG9jay5zaXplO1xuICAgICAgICAgICAgICAgICAgICB2YXIgYmkgPSAxO1xuICAgICAgICAgICAgICAgICAgICB3aGlsZSAoYmkgPCBibG9ja3NUb0ZldGNoLmxlbmd0aCAmJiBibG9ja3NUb0ZldGNoW2JpXS5vZmZzZXQgPT0gKGZldGNoU3RhcnQgKyBmZXRjaFNpemUpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBmZXRjaFNpemUgKz0gYmxvY2tzVG9GZXRjaFtiaV0uc2l6ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICsrYmk7XG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICB0aGlzQi5id2cuZGF0YS5zbGljZShmZXRjaFN0YXJ0LCBmZXRjaFNpemUpLmZldGNoKGZ1bmN0aW9uKHJlc3VsdCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIG9mZnNldCA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgYmkgPSAwO1xuICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKG9mZnNldCA8IGZldGNoU2l6ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBmYiA9IGJsb2Nrc1RvRmV0Y2hbYmldO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGRhdGE7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHRoaXNCLmJ3Zy51bmNvbXByZXNzQnVmU2l6ZSA+IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9IGpzemxpYl9pbmZsYXRlX2J1ZmZlcihyZXN1bHQsIG9mZnNldCArIDIsIGZiLnNpemUgLSAyKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgdG1wID0gbmV3IFVpbnQ4QXJyYXkoZmIuc2l6ZSk7ICAgIC8vIEZJWE1FIGlzIHRoaXMgcmVhbGx5IHRoZSBiZXN0IHdlIGNhbiBkbz9cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYXJyYXlDb3B5KG5ldyBVaW50OEFycmF5KHJlc3VsdCwgb2Zmc2V0LCBmYi5zaXplKSwgMCwgdG1wLCAwLCBmYi5zaXplKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZGF0YSA9IHRtcC5idWZmZXI7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZiLmRhdGEgPSBkYXRhO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIG9mZnNldCArPSBmYi5zaXplO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICsrYmk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICB0cmFtcCgpO1xuICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgdHJhbXAoKTtcbiAgICB9XG59XG5cbkJpZ1dpZ1ZpZXcucHJvdG90eXBlLnBhcnNlRmVhdHVyZXMgPSBmdW5jdGlvbihkYXRhLCBjcmVhdGVGZWF0dXJlLCBmaWx0ZXIpIHtcbiAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShkYXRhKTtcblxuICAgIGlmICh0aGlzLmlzU3VtbWFyeSkge1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShkYXRhKTtcbiAgICAgICAgdmFyIGxhID0gbmV3IEludDMyQXJyYXkoZGF0YSk7XG4gICAgICAgIHZhciBmYSA9IG5ldyBGbG9hdDMyQXJyYXkoZGF0YSk7XG5cbiAgICAgICAgdmFyIGl0ZW1Db3VudCA9IGRhdGEuYnl0ZUxlbmd0aC8zMjtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBpdGVtQ291bnQ7ICsraSkge1xuICAgICAgICAgICAgdmFyIGNocm9tSWQgPSAgIGxhWyhpKjgpXTtcbiAgICAgICAgICAgIHZhciBzdGFydCA9ICAgICBsYVsoaSo4KSsxXTtcbiAgICAgICAgICAgIHZhciBlbmQgPSAgICAgICBsYVsoaSo4KSsyXTtcbiAgICAgICAgICAgIHZhciB2YWxpZENudCA9ICBsYVsoaSo4KSszXTtcbiAgICAgICAgICAgIHZhciBtaW5WYWwgICAgPSBmYVsoaSo4KSs0XTtcbiAgICAgICAgICAgIHZhciBtYXhWYWwgICAgPSBmYVsoaSo4KSs1XTtcbiAgICAgICAgICAgIHZhciBzdW1EYXRhICAgPSBmYVsoaSo4KSs2XTtcbiAgICAgICAgICAgIHZhciBzdW1TcURhdGEgPSBmYVsoaSo4KSs3XTtcbiAgICAgICAgICAgIFxuICAgICAgICAgICAgaWYgKGZpbHRlcihjaHJvbUlkLCBzdGFydCArIDEsIGVuZCkpIHtcbiAgICAgICAgICAgICAgICB2YXIgc3VtbWFyeU9wdHMgPSB7dHlwZTogJ2JpZ3dpZycsIHNjb3JlOiBzdW1EYXRhL3ZhbGlkQ250LCBtYXhTY29yZTogbWF4VmFsfTtcbiAgICAgICAgICAgICAgICBpZiAodGhpcy5id2cudHlwZSA9PSAnYmlnYmVkJykge1xuICAgICAgICAgICAgICAgICAgICBzdW1tYXJ5T3B0cy50eXBlID0gJ2RlbnNpdHknO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIHN0YXJ0ICsgMSwgZW5kLCBzdW1tYXJ5T3B0cyk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9IGVsc2UgaWYgKHRoaXMuYndnLnR5cGUgPT0gJ2JpZ3dpZycpIHtcbiAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoZGF0YSk7XG4gICAgICAgIHZhciBsYSA9IG5ldyBJbnQzMkFycmF5KGRhdGEpO1xuICAgICAgICB2YXIgZmEgPSBuZXcgRmxvYXQzMkFycmF5KGRhdGEpO1xuXG4gICAgICAgIHZhciBjaHJvbUlkID0gbGFbMF07XG4gICAgICAgIHZhciBibG9ja1N0YXJ0ID0gbGFbMV07XG4gICAgICAgIHZhciBibG9ja0VuZCA9IGxhWzJdO1xuICAgICAgICB2YXIgaXRlbVN0ZXAgPSBsYVszXTtcbiAgICAgICAgdmFyIGl0ZW1TcGFuID0gbGFbNF07XG4gICAgICAgIHZhciBibG9ja1R5cGUgPSBiYVsyMF07XG4gICAgICAgIHZhciBpdGVtQ291bnQgPSBzYVsxMV07XG4gICAgICAgIFxuICAgICAgICBpZiAoYmxvY2tUeXBlID09IEJJR19XSUdfVFlQRV9GU1RFUCkge1xuICAgICAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBpdGVtQ291bnQ7ICsraSkge1xuICAgICAgICAgICAgICAgIHZhciBzY29yZSA9IGZhW2kgKyA2XTtcbiAgICAgICAgICAgICAgICB2YXIgZm1pbiA9IGJsb2NrU3RhcnQgKyAoaSppdGVtU3RlcCkgKyAxLCBmbWF4ID0gYmxvY2tTdGFydCArIChpKml0ZW1TdGVwKSArIGl0ZW1TcGFuO1xuICAgICAgICAgICAgICAgIGlmIChmaWx0ZXIoY2hyb21JZCwgZm1pbiwgZm1heCkpXG4gICAgICAgICAgICAgICAgICAgIGNyZWF0ZUZlYXR1cmUoY2hyb21JZCwgZm1pbiwgZm1heCwge3Njb3JlOiBzY29yZX0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9IGVsc2UgaWYgKGJsb2NrVHlwZSA9PSBCSUdfV0lHX1RZUEVfVlNURVApIHtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgaXRlbUNvdW50OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnQgPSBsYVsoaSoyKSArIDZdICsgMTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kID0gc3RhcnQgKyBpdGVtU3BhbiAtIDE7XG4gICAgICAgICAgICAgICAgdmFyIHNjb3JlID0gZmFbKGkqMikgKyA3XTtcbiAgICAgICAgICAgICAgICBpZiAoZmlsdGVyKGNocm9tSWQsIHN0YXJ0LCBlbmQpKVxuICAgICAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIHN0YXJ0LCBlbmQsIHtzY29yZTogc2NvcmV9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSBlbHNlIGlmIChibG9ja1R5cGUgPT0gQklHX1dJR19UWVBFX0dSQVBIKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGl0ZW1Db3VudDsgKytpKSB7XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0ID0gbGFbKGkqMykgKyA2XSArIDE7XG4gICAgICAgICAgICAgICAgdmFyIGVuZCAgID0gbGFbKGkqMykgKyA3XTtcbiAgICAgICAgICAgICAgICB2YXIgc2NvcmUgPSBmYVsoaSozKSArIDhdO1xuICAgICAgICAgICAgICAgIGlmIChzdGFydCA+IGVuZCkge1xuICAgICAgICAgICAgICAgICAgICBzdGFydCA9IGVuZDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgaWYgKGZpbHRlcihjaHJvbUlkLCBzdGFydCwgZW5kKSlcbiAgICAgICAgICAgICAgICAgICAgY3JlYXRlRmVhdHVyZShjaHJvbUlkLCBzdGFydCwgZW5kLCB7c2NvcmU6IHNjb3JlfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjb25zb2xlLmxvZygnQ3VycmVudGx5IG5vdCBoYW5kbGluZyBid2dUeXBlPScgKyBibG9ja1R5cGUpO1xuICAgICAgICB9XG4gICAgfSBlbHNlIGlmICh0aGlzLmJ3Zy50eXBlID09ICdiaWdiZWQnKSB7XG4gICAgICAgIHZhciBvZmZzZXQgPSAwO1xuICAgICAgICB2YXIgZGZjID0gdGhpcy5id2cuZGVmaW5lZEZpZWxkQ291bnQ7XG4gICAgICAgIHZhciBzY2hlbWEgPSB0aGlzLmJ3Zy5zY2hlbWE7XG5cbiAgICAgICAgd2hpbGUgKG9mZnNldCA8IGJhLmxlbmd0aCkge1xuICAgICAgICAgICAgdmFyIGNocm9tSWQgPSAoYmFbb2Zmc2V0KzNdPDwyNCkgfCAoYmFbb2Zmc2V0KzJdPDwxNikgfCAoYmFbb2Zmc2V0KzFdPDw4KSB8IChiYVtvZmZzZXQrMF0pO1xuICAgICAgICAgICAgdmFyIHN0YXJ0ID0gKGJhW29mZnNldCs3XTw8MjQpIHwgKGJhW29mZnNldCs2XTw8MTYpIHwgKGJhW29mZnNldCs1XTw8OCkgfCAoYmFbb2Zmc2V0KzRdKTtcbiAgICAgICAgICAgIHZhciBlbmQgPSAoYmFbb2Zmc2V0KzExXTw8MjQpIHwgKGJhW29mZnNldCsxMF08PDE2KSB8IChiYVtvZmZzZXQrOV08PDgpIHwgKGJhW29mZnNldCs4XSk7XG4gICAgICAgICAgICBvZmZzZXQgKz0gMTI7XG4gICAgICAgICAgICB2YXIgcmVzdCA9ICcnO1xuICAgICAgICAgICAgd2hpbGUgKHRydWUpIHtcbiAgICAgICAgICAgICAgICB2YXIgY2ggPSBiYVtvZmZzZXQrK107XG4gICAgICAgICAgICAgICAgaWYgKGNoICE9IDApIHtcbiAgICAgICAgICAgICAgICAgICAgcmVzdCArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKGNoKTtcbiAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICBicmVhaztcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIHZhciBmZWF0dXJlT3B0cyA9IHt9O1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgYmVkQ29sdW1ucztcbiAgICAgICAgICAgIGlmIChyZXN0Lmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgICAgICBiZWRDb2x1bW5zID0gcmVzdC5zcGxpdCgnXFx0Jyk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIGJlZENvbHVtbnMgPSBbXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDAgJiYgZGZjID4gMykge1xuICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLmxhYmVsID0gYmVkQ29sdW1uc1swXTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDEgJiYgZGZjID4gNCkge1xuICAgICAgICAgICAgICAgIHZhciBzY29yZSA9IHBhcnNlSW50KGJlZENvbHVtbnNbMV0pO1xuICAgICAgICAgICAgICAgIGlmICghaXNOYU4oc2NvcmUpKVxuICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5zY29yZSA9IHNjb3JlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKGJlZENvbHVtbnMubGVuZ3RoID4gMiAmJiBkZmMgPiA1KSB7XG4gICAgICAgICAgICAgICAgZmVhdHVyZU9wdHMub3JpZW50YXRpb24gPSBiZWRDb2x1bW5zWzJdO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKGJlZENvbHVtbnMubGVuZ3RoID4gNSAmJiBkZmMgPiA4KSB7XG4gICAgICAgICAgICAgICAgdmFyIGNvbG9yID0gYmVkQ29sdW1uc1s1XTtcbiAgICAgICAgICAgICAgICBpZiAoQkVEX0NPTE9SX1JFR0VYUC50ZXN0KGNvbG9yKSkge1xuICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5pdGVtUmdiID0gJ3JnYignICsgY29sb3IgKyAnKSc7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICBpZiAoYmVkQ29sdW1ucy5sZW5ndGggPiBkZmMtMyAmJiBzY2hlbWEpIHtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBjb2wgPSBkZmMgLSAzOyBjb2wgPCBiZWRDb2x1bW5zLmxlbmd0aDsgKytjb2wpIHtcbiAgICAgICAgICAgICAgICAgICAgZmVhdHVyZU9wdHNbc2NoZW1hLmZpZWxkc1tjb2wrM10ubmFtZV0gPSBiZWRDb2x1bW5zW2NvbF07XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICBpZiAoZmlsdGVyKGNocm9tSWQsIHN0YXJ0ICsgMSwgZW5kLCBiZWRDb2x1bW5zKSkge1xuICAgICAgICAgICAgICAgIGlmIChkZmMgPCAxMikge1xuICAgICAgICAgICAgICAgICAgICBjcmVhdGVGZWF0dXJlKGNocm9tSWQsIHN0YXJ0ICsgMSwgZW5kLCBmZWF0dXJlT3B0cyk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHRoaWNrU3RhcnQgPSBiZWRDb2x1bW5zWzNdfDA7XG4gICAgICAgICAgICAgICAgICAgIHZhciB0aGlja0VuZCAgID0gYmVkQ29sdW1uc1s0XXwwO1xuICAgICAgICAgICAgICAgICAgICB2YXIgYmxvY2tDb3VudCA9IGJlZENvbHVtbnNbNl18MDtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGJsb2NrU2l6ZXMgPSBiZWRDb2x1bW5zWzddLnNwbGl0KCcsJyk7XG4gICAgICAgICAgICAgICAgICAgIHZhciBibG9ja1N0YXJ0cyA9IGJlZENvbHVtbnNbOF0uc3BsaXQoJywnKTtcblxuICAgICAgICAgICAgICAgICAgICBpZiAoZmVhdHVyZU9wdHMuZXhvbkZyYW1lcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGV4b25GcmFtZXMgPSBmZWF0dXJlT3B0cy5leG9uRnJhbWVzLnNwbGl0KCcsJyk7XG4gICAgICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5leG9uRnJhbWVzID0gdW5kZWZpbmVkO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy50eXBlID0gJ3RyYW5zY3JpcHQnXG4gICAgICAgICAgICAgICAgICAgIHZhciBncnAgPSBuZXcgREFTR3JvdXAoKTtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgayBpbiBmZWF0dXJlT3B0cykge1xuICAgICAgICAgICAgICAgICAgICAgICAgZ3JwW2tdID0gZmVhdHVyZU9wdHNba107XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZ3JwLmlkID0gYmVkQ29sdW1uc1swXTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLnNlZ21lbnQgPSB0aGlzLmJ3Zy5pZHNUb0Nocm9tc1tjaHJvbUlkXTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm1pbiA9IHN0YXJ0ICsgMTtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm1heCA9IGVuZDtcbiAgICAgICAgICAgICAgICAgICAgZ3JwLm5vdGVzID0gW107XG4gICAgICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLmdyb3VwcyA9IFtncnBdO1xuXG4gICAgICAgICAgICAgICAgICAgIC8vIE1vdmluZyB0b3dhcmRzIHVzaW5nIGJpZ0dlbmVQcmVkIG1vZGVsLCBidXQgd2lsbFxuICAgICAgICAgICAgICAgICAgICAvLyBzdGlsbCBzdXBwb3J0IG9sZCBEYWxsaWFuY2Utc3R5bGUgQkVEMTIrZ2VuZS1uYW1lIGZvciB0aGVcbiAgICAgICAgICAgICAgICAgICAgLy8gZm9yZXNlZWFibGUgZnV0dXJlLlxuICAgICAgICAgICAgICAgICAgICBpZiAoYmVkQ29sdW1ucy5sZW5ndGggPiA5KSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgZ2VuZUlkID0gZmVhdHVyZU9wdHMuZ2VuZU5hbWUgfHwgYmVkQ29sdW1uc1s5XTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBnZW5lTmFtZSA9IGdlbmVJZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChiZWRDb2x1bW5zLmxlbmd0aCA+IDEwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZ2VuZU5hbWUgPSBiZWRDb2x1bW5zWzEwXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChmZWF0dXJlT3B0cy5nZW5lTmFtZTIpXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgZ2VuZU5hbWUgPSBmZWF0dXJlT3B0cy5nZW5lTmFtZTI7XG5cbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBnZyA9IHNoYWxsb3dDb3B5KGdycCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBnZy5pZCA9IGdlbmVJZDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGdnLmxhYmVsID0gZ2VuZU5hbWU7XG4gICAgICAgICAgICAgICAgICAgICAgICBnZy50eXBlID0gJ2dlbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgZmVhdHVyZU9wdHMuZ3JvdXBzLnB1c2goZ2cpO1xuICAgICAgICAgICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICAgICAgdmFyIHNwYW5MaXN0ID0gW107XG4gICAgICAgICAgICAgICAgICAgIGZvciAodmFyIGIgPSAwOyBiIDwgYmxvY2tDb3VudDsgKytiKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgYm1pbiA9IChibG9ja1N0YXJ0c1tiXXwwKSArIHN0YXJ0O1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGJtYXggPSBibWluICsgKGJsb2NrU2l6ZXNbYl18MCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgc3BhbiA9IG5ldyBSYW5nZShibWluLCBibWF4KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHNwYW5MaXN0LnB1c2goc3Bhbik7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgdmFyIHNwYW5zID0gdW5pb24oc3Bhbkxpc3QpO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgdmFyIHRzTGlzdCA9IHNwYW5zLnJhbmdlcygpO1xuICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBzID0gMDsgcyA8IHRzTGlzdC5sZW5ndGg7ICsrcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHRzID0gdHNMaXN0W3NdO1xuICAgICAgICAgICAgICAgICAgICAgICAgY3JlYXRlRmVhdHVyZShjaHJvbUlkLCB0cy5taW4oKSArIDEsIHRzLm1heCgpLCBmZWF0dXJlT3B0cyk7XG4gICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICBpZiAodGhpY2tFbmQgPiB0aGlja1N0YXJ0KSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgY29kaW5nUmVnaW9uID0gKGZlYXR1cmVPcHRzLm9yaWVudGF0aW9uID09ICcrJykgP1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIG5ldyBSYW5nZSh0aGlja1N0YXJ0LCB0aGlja0VuZCArIDMpIDpcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBuZXcgUmFuZ2UodGhpY2tTdGFydCAtIDMsIHRoaWNrRW5kKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyArLy0gMyB0byBhY2NvdW50IGZvciBzdG9wIGNvZG9uXG5cbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciB0bCA9IGludGVyc2VjdGlvbihzcGFucywgY29kaW5nUmVnaW9uKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0bCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLnR5cGUgPSAndHJhbnNsYXRpb24nO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciB0bExpc3QgPSB0bC5yYW5nZXMoKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgcmVhZGluZ0ZyYW1lID0gMDtcblxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciB0bE9mZnNldCA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgd2hpbGUgKHRsTGlzdFswXS5taW4oKSA+IHRzTGlzdFt0bE9mZnNldF0ubWF4KCkpXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHRsT2Zmc2V0Kys7XG5cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBmb3IgKHZhciBzID0gMDsgcyA8IHRsTGlzdC5sZW5ndGg7ICsrcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyBSZWNvcmQgcmVhZGluZyBmcmFtZSBmb3IgZXZlcnkgZXhvblxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgaW5kZXggPSBzO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoZmVhdHVyZU9wdHMub3JpZW50YXRpb24gPT0gJy0nKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW5kZXggPSB0bExpc3QubGVuZ3RoIC0gcyAtIDE7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciB0cyA9IHRsTGlzdFtpbmRleF07XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZlYXR1cmVPcHRzLnJlYWRmcmFtZSA9IHJlYWRpbmdGcmFtZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGV4b25GcmFtZXMpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBicmYgPSBwYXJzZUludChleG9uRnJhbWVzW2luZGV4ICsgdGxPZmZzZXRdKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0eXBlb2YoYnJmKSA9PT0gJ251bWJlcicgJiYgYnJmID49IDAgJiYgYnJmIDw9IDIpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBmZWF0dXJlT3B0cy5yZWFkZnJhbWUgPSBicmY7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZmVhdHVyZU9wdHMucmVhZGZyYW1lRXhwbGljaXQgPSB0cnVlO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBsZW5ndGggPSB0cy5tYXgoKSAtIHRzLm1pbigpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZWFkaW5nRnJhbWUgPSAocmVhZGluZ0ZyYW1lICsgbGVuZ3RoKSAlIDM7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNyZWF0ZUZlYXR1cmUoY2hyb21JZCwgdHMubWluKCkgKyAxLCB0cy5tYXgoKSwgZmVhdHVyZU9wdHMpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICAgIHRocm93IEVycm9yKFwiRG9uJ3Qga25vdyB3aGF0IHRvIGRvIHdpdGggXCIgKyB0aGlzLmJ3Zy50eXBlKTtcbiAgICB9XG59XG5cbi8vXG4vLyBuYXN0eSBjdXQvcGFzdGUsIHNob3VsZCByb2xsIGJhY2sgaW4hXG4vL1xuXG5CaWdXaWdWaWV3LnByb3RvdHlwZS5nZXRGaXJzdEFkamFjZW50ID0gZnVuY3Rpb24oY2hyTmFtZSwgcG9zLCBkaXIsIGNhbGxiYWNrKSB7XG4gICAgdmFyIGNociA9IHRoaXMuYndnLmNocm9tc1RvSURzW2Nock5hbWVdO1xuICAgIGlmIChjaHIgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICAvLyBOb3QgYW4gZXJyb3IgYmVjYXVzZSBzb21lIC5id2dzIHdvbid0IGhhdmUgZGF0YSBmb3IgYWxsIGNocm9tb3NvbWVzLlxuICAgICAgICByZXR1cm4gY2FsbGJhY2soW10pO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHRoaXMuZ2V0Rmlyc3RBZGphY2VudEJ5SWQoY2hyLCBwb3MsIGRpciwgY2FsbGJhY2spO1xuICAgIH1cbn1cblxuQmlnV2lnVmlldy5wcm90b3R5cGUuZ2V0Rmlyc3RBZGphY2VudEJ5SWQgPSBmdW5jdGlvbihjaHIsIHBvcywgZGlyLCBjYWxsYmFjaykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG4gICAgaWYgKCF0aGlzLmNpckhlYWRlcikge1xuICAgICAgICB0aGlzLmJ3Zy5kYXRhLnNsaWNlKHRoaXMuY2lyVHJlZU9mZnNldCwgNDgpLmZldGNoKGZ1bmN0aW9uKHJlc3VsdCkge1xuICAgICAgICAgICAgdGhpc0IuY2lySGVhZGVyID0gcmVzdWx0O1xuICAgICAgICAgICAgdmFyIGxhID0gbmV3IEludDMyQXJyYXkodGhpc0IuY2lySGVhZGVyKTtcbiAgICAgICAgICAgIHRoaXNCLmNpckJsb2NrU2l6ZSA9IGxhWzFdO1xuICAgICAgICAgICAgdGhpc0IuZ2V0Rmlyc3RBZGphY2VudEJ5SWQoY2hyLCBwb3MsIGRpciwgY2FsbGJhY2spO1xuICAgICAgICB9KTtcbiAgICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIHZhciBibG9ja1RvRmV0Y2ggPSBudWxsO1xuICAgIHZhciBiZXN0QmxvY2tDaHIgPSAtMTtcbiAgICB2YXIgYmVzdEJsb2NrT2Zmc2V0ID0gLTE7XG5cbiAgICB2YXIgb3V0c3RhbmRpbmcgPSAwO1xuXG4gICAgdmFyIGJlZm9yZUJXRyA9IERhdGUubm93KCk7XG5cbiAgICB2YXIgY2lyRm9iUmVjdXIgPSBmdW5jdGlvbihvZmZzZXQsIGxldmVsKSB7XG4gICAgICAgIG91dHN0YW5kaW5nICs9IG9mZnNldC5sZW5ndGg7XG5cbiAgICAgICAgdmFyIG1heENpckJsb2NrU3BhbiA9IDQgKyAgKHRoaXNCLmNpckJsb2NrU2l6ZSAqIDMyKTsgICAvLyBVcHBlciBib3VuZCBvbiBzaXplLCBiYXNlZCBvbiBhIGNvbXBsZXRlbHkgZnVsbCBsZWFmIG5vZGUuXG4gICAgICAgIHZhciBzcGFucztcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBvZmZzZXQubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgIHZhciBibG9ja1NwYW4gPSBuZXcgUmFuZ2Uob2Zmc2V0W2ldLCBvZmZzZXRbaV0gKyBtYXhDaXJCbG9ja1NwYW4pO1xuICAgICAgICAgICAgc3BhbnMgPSBzcGFucyA/IHVuaW9uKHNwYW5zLCBibG9ja1NwYW4pIDogYmxvY2tTcGFuO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICB2YXIgZmV0Y2hSYW5nZXMgPSBzcGFucy5yYW5nZXMoKTtcbiAgICAgICAgZm9yICh2YXIgciA9IDA7IHIgPCBmZXRjaFJhbmdlcy5sZW5ndGg7ICsrcikge1xuICAgICAgICAgICAgdmFyIGZyID0gZmV0Y2hSYW5nZXNbcl07XG4gICAgICAgICAgICBjaXJGb2JTdGFydEZldGNoKG9mZnNldCwgZnIsIGxldmVsKTtcbiAgICAgICAgfVxuICAgIH1cblxuICAgIHZhciBjaXJGb2JTdGFydEZldGNoID0gZnVuY3Rpb24ob2Zmc2V0LCBmciwgbGV2ZWwsIGF0dGVtcHRzKSB7XG4gICAgICAgIHZhciBsZW5ndGggPSBmci5tYXgoKSAtIGZyLm1pbigpO1xuICAgICAgICB0aGlzQi5id2cuZGF0YS5zbGljZShmci5taW4oKSwgZnIubWF4KCkgLSBmci5taW4oKSkuZmV0Y2goZnVuY3Rpb24ocmVzdWx0QnVmZmVyKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IG9mZnNldC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgICAgIGlmIChmci5jb250YWlucyhvZmZzZXRbaV0pKSB7XG4gICAgICAgICAgICAgICAgICAgIGNpckZvYlJlY3VyMihyZXN1bHRCdWZmZXIsIG9mZnNldFtpXSAtIGZyLm1pbigpLCBsZXZlbCk7XG4gICAgICAgICAgICAgICAgICAgIC0tb3V0c3RhbmRpbmc7XG4gICAgICAgICAgICAgICAgICAgIGlmIChvdXRzdGFuZGluZyA9PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoIWJsb2NrVG9GZXRjaCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmIChkaXIgPiAwICYmIChjaHIgIT0gMCB8fCBwb3MgPiAwKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gdGhpc0IuZ2V0Rmlyc3RBZGphY2VudEJ5SWQoMCwgMCwgZGlyLCBjYWxsYmFjayk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIGlmIChkaXIgPCAwICYmIChjaHIgIT0gdGhpc0IuYndnLm1heElEIHx8IHBvcyA8IDEwMDAwMDAwMDApKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0aGlzQi5nZXRGaXJzdEFkamFjZW50QnlJZCh0aGlzQi5id2cubWF4SUQsIDEwMDAwMDAwMDAsIGRpciwgY2FsbGJhY2spO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soW10pO1xuICAgICAgICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgICAgICAgICB0aGlzQi5mZXRjaEZlYXR1cmVzKGZ1bmN0aW9uKGNocngsIGZtaW4sIGZtYXgsIHRva3MpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gKGRpciA8IDAgJiYgKGNocnggPCBjaHIgfHwgZm1heCA8IHBvcykpIHx8IChkaXIgPiAwICYmIChjaHJ4ID4gY2hyIHx8IGZtaW4gPiBwb3MpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0sIFtibG9ja1RvRmV0Y2hdLCBmdW5jdGlvbihmZWF0dXJlcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBiZXN0RmVhdHVyZSA9IG51bGw7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGJlc3RDaHIgPSAtMTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgYmVzdFBvcyA9IC0xO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZvciAodmFyIGZpID0gMDsgZmkgPCBmZWF0dXJlcy5sZW5ndGg7ICsrZmkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGYgPSBmZWF0dXJlc1tmaV07XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBjaHJ4ID0gZi5fY2hyb21JZCwgZm1pbiA9IGYubWluLCBmbWF4ID0gZi5tYXg7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmIChiZXN0RmVhdHVyZSA9PSBudWxsIHx8ICgoZGlyIDwgMCkgJiYgKGNocnggPiBiZXN0Q2hyIHx8IGZtYXggPiBiZXN0UG9zKSkgfHwgKChkaXIgPiAwKSAmJiAoY2hyeCA8IGJlc3RDaHIgfHwgZm1pbiA8IGJlc3RQb3MpKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYmVzdEZlYXR1cmUgPSBmO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYmVzdFBvcyA9IChkaXIgPCAwKSA/IGZtYXggOiBmbWluO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgYmVzdENociA9IGNocng7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9XG5cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBpZiAoYmVzdEZlYXR1cmUgIT0gbnVsbCkgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhbYmVzdEZlYXR1cmVdKTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBlbHNlXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhbXSk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgfVxuXG4gICAgdmFyIGNpckZvYlJlY3VyMiA9IGZ1bmN0aW9uKGNpckJsb2NrRGF0YSwgb2Zmc2V0LCBsZXZlbCkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShjaXJCbG9ja0RhdGEpO1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShjaXJCbG9ja0RhdGEpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShjaXJCbG9ja0RhdGEpO1xuXG4gICAgICAgIHZhciBpc0xlYWYgPSBiYVtvZmZzZXRdO1xuICAgICAgICB2YXIgY250ID0gc2Fbb2Zmc2V0LzIgKyAxXTtcbiAgICAgICAgb2Zmc2V0ICs9IDQ7XG5cbiAgICAgICAgaWYgKGlzTGVhZiAhPSAwKSB7XG4gICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IGNudDsgKytpKSB7XG4gICAgICAgICAgICAgICAgdmFyIGxvID0gb2Zmc2V0LzQ7XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0Q2hyb20gPSBsYVtsb107XG4gICAgICAgICAgICAgICAgdmFyIHN0YXJ0QmFzZSA9IGxhW2xvICsgMV07XG4gICAgICAgICAgICAgICAgdmFyIGVuZENocm9tID0gbGFbbG8gKyAyXTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kQmFzZSA9IGxhW2xvICsgM107XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIG9mZnNldCsxNik7XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrU2l6ZSA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCBvZmZzZXQrMjQpO1xuICAgICAgICAgICAgICAgIGlmICgoZGlyIDwgMCAmJiAoKHN0YXJ0Q2hyb20gPCBjaHIgfHwgKHN0YXJ0Q2hyb20gPT0gY2hyICYmIHN0YXJ0QmFzZSA8PSBwb3MpKSkpIHx8XG4gICAgICAgICAgICAgICAgICAgIChkaXIgPiAwICYmICgoZW5kQ2hyb20gPiBjaHIgfHwgKGVuZENocm9tID09IGNociAmJiBlbmRCYXNlID49IHBvcykpKSkpXG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAvLyBjb25zb2xlLmxvZygnR290IGFuIGludGVyZXN0aW5nIGJsb2NrOiBzdGFydEJhc2U9JyArIHN0YXJ0Q2hyb20gKyAnOicgKyBzdGFydEJhc2UgKyAnOyBlbmRCYXNlPScgKyBlbmRDaHJvbSArICc6JyArIGVuZEJhc2UgKyAnOyBvZmZzZXQ9JyArIGJsb2NrT2Zmc2V0ICsgJzsgc2l6ZT0nICsgYmxvY2tTaXplKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKC9fcmFuZG9tLy5leGVjKHRoaXNCLmJ3Zy5pZHNUb0Nocm9tc1tzdGFydENocm9tXSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vIGRsb2coJ3NraXBwaW5nIHJhbmRvbTogJyArIHRoaXNCLmJ3Zy5pZHNUb0Nocm9tc1tzdGFydENocm9tXSk7XG4gICAgICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAoYmxvY2tUb0ZldGNoID09IG51bGwgfHwgKChkaXIgPCAwKSAmJiAoZW5kQ2hyb20gPiBiZXN0QmxvY2tDaHIgfHwgKGVuZENocm9tID09IGJlc3RCbG9ja0NociAmJiBlbmRCYXNlID4gYmVzdEJsb2NrT2Zmc2V0KSkgfHxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAoZGlyID4gMCkgJiYgKHN0YXJ0Q2hyb20gPCBiZXN0QmxvY2tDaHIgfHwgKHN0YXJ0Q2hyb20gPT0gYmVzdEJsb2NrQ2hyICYmIHN0YXJ0QmFzZSA8IGJlc3RCbG9ja09mZnNldCkpKSlcbiAgICAgICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gICAgICAgICAgICAgICAgICAgICAgICBkbG9nKCdiZXN0IGlzOiBzdGFydEJhc2U9JyArIHN0YXJ0Q2hyb20gKyAnOicgKyBzdGFydEJhc2UgKyAnOyBlbmRCYXNlPScgKyBlbmRDaHJvbSArICc6JyArIGVuZEJhc2UgKyAnOyBvZmZzZXQ9JyArIGJsb2NrT2Zmc2V0ICsgJzsgc2l6ZT0nICsgYmxvY2tTaXplKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJsb2NrVG9GZXRjaCA9IHtvZmZzZXQ6IGJsb2NrT2Zmc2V0LCBzaXplOiBibG9ja1NpemV9O1xuICAgICAgICAgICAgICAgICAgICAgICAgYmVzdEJsb2NrT2Zmc2V0ID0gKGRpciA8IDApID8gZW5kQmFzZSA6IHN0YXJ0QmFzZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJlc3RCbG9ja0NociA9IChkaXIgPCAwKSA/IGVuZENocm9tIDogc3RhcnRDaHJvbTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBvZmZzZXQgKz0gMzI7XG4gICAgICAgICAgICB9XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICB2YXIgYmVzdFJlY3VyID0gLTE7XG4gICAgICAgICAgICB2YXIgYmVzdFBvcyA9IC0xO1xuICAgICAgICAgICAgdmFyIGJlc3RDaHIgPSAtMTtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY250OyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgbG8gPSBvZmZzZXQvNDtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRDaHJvbSA9IGxhW2xvXTtcbiAgICAgICAgICAgICAgICB2YXIgc3RhcnRCYXNlID0gbGFbbG8gKyAxXTtcbiAgICAgICAgICAgICAgICB2YXIgZW5kQ2hyb20gPSBsYVtsbyArIDJdO1xuICAgICAgICAgICAgICAgIHZhciBlbmRCYXNlID0gbGFbbG8gKyAzXTtcbiAgICAgICAgICAgICAgICB2YXIgYmxvY2tPZmZzZXQgPSAobGFbbG8gKyA0XTw8MzIpIHwgKGxhW2xvICsgNV0pO1xuICAgICAgICAgICAgICAgIGlmICgoZGlyIDwgMCAmJiAoKHN0YXJ0Q2hyb20gPCBjaHIgfHwgKHN0YXJ0Q2hyb20gPT0gY2hyICYmIHN0YXJ0QmFzZSA8PSBwb3MpKSAmJlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgKGVuZENocm9tICAgPj0gY2hyKSkpIHx8XG4gICAgICAgICAgICAgICAgICAgICAoZGlyID4gMCAmJiAoKGVuZENocm9tID4gY2hyIHx8IChlbmRDaHJvbSA9PSBjaHIgJiYgZW5kQmFzZSA+PSBwb3MpKSAmJlxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIChzdGFydENocm9tIDw9IGNocikpKSlcbiAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChiZXN0UmVjdXIgPCAwIHx8IGVuZEJhc2UgPiBiZXN0UG9zKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBiZXN0UmVjdXIgPSBibG9ja09mZnNldDtcbiAgICAgICAgICAgICAgICAgICAgICAgIGJlc3RQb3MgPSAoZGlyIDwgMCkgPyBlbmRCYXNlIDogc3RhcnRCYXNlO1xuICAgICAgICAgICAgICAgICAgICAgICAgYmVzdENociA9IChkaXIgPCAwKSA/IGVuZENocm9tIDogc3RhcnRDaHJvbTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBvZmZzZXQgKz0gMjQ7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBpZiAoYmVzdFJlY3VyID49IDApIHtcbiAgICAgICAgICAgICAgICBjaXJGb2JSZWN1cihbYmVzdFJlY3VyXSwgbGV2ZWwgKyAxKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH07XG4gICAgXG5cbiAgICBjaXJGb2JSZWN1cihbdGhpc0IuY2lyVHJlZU9mZnNldCArIDQ4XSwgMSk7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUucmVhZFdpZ0RhdGEgPSBmdW5jdGlvbihjaHJOYW1lLCBtaW4sIG1heCwgY2FsbGJhY2spIHtcbiAgICB0aGlzLmdldFVuem9vbWVkVmlldygpLnJlYWRXaWdEYXRhKGNock5hbWUsIG1pbiwgbWF4LCBjYWxsYmFjayk7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUuZ2V0VW56b29tZWRWaWV3ID0gZnVuY3Rpb24oKSB7XG4gICAgaWYgKCF0aGlzLnVuem9vbWVkVmlldykge1xuICAgICAgICB2YXIgY2lyTGVuID0gNDAwMDtcbiAgICAgICAgdmFyIG56bCA9IHRoaXMuem9vbUxldmVsc1swXTtcbiAgICAgICAgaWYgKG56bCkge1xuICAgICAgICAgICAgY2lyTGVuID0gdGhpcy56b29tTGV2ZWxzWzBdLmRhdGFPZmZzZXQgLSB0aGlzLnVuem9vbWVkSW5kZXhPZmZzZXQ7XG4gICAgICAgIH1cbiAgICAgICAgdGhpcy51bnpvb21lZFZpZXcgPSBuZXcgQmlnV2lnVmlldyh0aGlzLCB0aGlzLnVuem9vbWVkSW5kZXhPZmZzZXQsIGNpckxlbiwgZmFsc2UpO1xuICAgIH1cbiAgICByZXR1cm4gdGhpcy51bnpvb21lZFZpZXc7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUuZ2V0Wm9vbWVkVmlldyA9IGZ1bmN0aW9uKHopIHtcbiAgICB2YXIgemggPSB0aGlzLnpvb21MZXZlbHNbel07XG4gICAgaWYgKCF6aC52aWV3KSB7XG4gICAgICAgIHpoLnZpZXcgPSBuZXcgQmlnV2lnVmlldyh0aGlzLCB6aC5pbmRleE9mZnNldCwgLyogdGhpcy56b29tTGV2ZWxzW3ogKyAxXS5kYXRhT2Zmc2V0IC0gemguaW5kZXhPZmZzZXQgKi8gNDAwMCwgdHJ1ZSk7XG4gICAgfVxuICAgIHJldHVybiB6aC52aWV3O1xufVxuXG5mdW5jdGlvbiBtYWtlQndnKGRhdGEsIGNhbGxiYWNrLCBuYW1lKSB7XG4gICAgdmFyIGJ3ZyA9IG5ldyBCaWdXaWcoKTtcbiAgICBid2cuZGF0YSA9IGRhdGE7XG4gICAgYndnLm5hbWUgPSBuYW1lO1xuICAgIGJ3Zy5kYXRhLnNsaWNlKDAsIDUxMikuc2FsdGVkKCkuZmV0Y2goZnVuY3Rpb24ocmVzdWx0KSB7XG4gICAgICAgIGlmICghcmVzdWx0KSB7XG4gICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCwgXCJDb3VsZG4ndCBmZXRjaCBmaWxlXCIpO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIGhlYWRlciA9IHJlc3VsdDtcbiAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoaGVhZGVyKTtcbiAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoaGVhZGVyKTtcbiAgICAgICAgdmFyIGxhID0gbmV3IEludDMyQXJyYXkoaGVhZGVyKTtcbiAgICAgICAgdmFyIG1hZ2ljID0gYmFbMF0gKyAoTTEgKiBiYVsxXSkgKyAoTTIgKiBiYVsyXSkgKyAoTTMgKiBiYVszXSk7XG4gICAgICAgIGlmIChtYWdpYyA9PSBCSUdfV0lHX01BR0lDKSB7XG4gICAgICAgICAgICBid2cudHlwZSA9ICdiaWd3aWcnO1xuICAgICAgICB9IGVsc2UgaWYgKG1hZ2ljID09IEJJR19CRURfTUFHSUMpIHtcbiAgICAgICAgICAgIGJ3Zy50eXBlID0gJ2JpZ2JlZCc7XG4gICAgICAgIH0gZWxzZSBpZiAobWFnaWMgPT0gQklHX1dJR19NQUdJQ19CRSB8fCBtYWdpYyA9PSBCSUdfQkVEX01BR0lDX0JFKSB7XG4gICAgICAgICAgICBjYWxsYmFjayhudWxsLCBcIkN1cnJlbnRseSBkb24ndCBzdXBwb3J0IGJpZy1lbmRpYW4gQkJJIGZpbGVzXCIpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY2FsbGJhY2sobnVsbCwgXCJOb3QgYSBzdXBwb3J0ZWQgZm9ybWF0LCBtYWdpYz0weFwiICsgbWFnaWMudG9TdHJpbmcoMTYpKTtcbiAgICAgICAgfVxuXG4gICAgICAgIGJ3Zy52ZXJzaW9uID0gc2FbMl07ICAgICAgICAgICAgIC8vIDRcbiAgICAgICAgYndnLm51bVpvb21MZXZlbHMgPSBzYVszXTsgICAgICAgLy8gNlxuICAgICAgICBid2cuY2hyb21UcmVlT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIDgpO1xuICAgICAgICBid2cudW56b29tZWREYXRhT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIDE2KTtcbiAgICAgICAgYndnLnVuem9vbWVkSW5kZXhPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgMjQpO1xuICAgICAgICBid2cuZmllbGRDb3VudCA9IHNhWzE2XTsgICAgICAgICAvLyAzMlxuICAgICAgICBid2cuZGVmaW5lZEZpZWxkQ291bnQgPSBzYVsxN107ICAvLyAzNFxuICAgICAgICBid2cuYXNPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgMzYpO1xuICAgICAgICBid2cudG90YWxTdW1tYXJ5T2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIDQ0KTtcbiAgICAgICAgYndnLnVuY29tcHJlc3NCdWZTaXplID0gbGFbMTNdOyAgLy8gNTJcbiAgICAgICAgYndnLmV4dEhlYWRlck9mZnNldCA9IGJ3Z19yZWFkT2Zmc2V0KGJhLCA1Nik7XG5cbiAgICAgICAgYndnLnpvb21MZXZlbHMgPSBbXTtcbiAgICAgICAgZm9yICh2YXIgemwgPSAwOyB6bCA8IGJ3Zy5udW1ab29tTGV2ZWxzOyArK3psKSB7XG4gICAgICAgICAgICB2YXIgemxSZWR1Y3Rpb24gPSBsYVt6bCo2ICsgMTZdXG4gICAgICAgICAgICB2YXIgemxEYXRhID0gYndnX3JlYWRPZmZzZXQoYmEsIHpsKjI0ICsgNzIpO1xuICAgICAgICAgICAgdmFyIHpsSW5kZXggPSBid2dfcmVhZE9mZnNldChiYSwgemwqMjQgKyA4MCk7XG4gICAgICAgICAgICBid2cuem9vbUxldmVscy5wdXNoKHtyZWR1Y3Rpb246IHpsUmVkdWN0aW9uLCBkYXRhT2Zmc2V0OiB6bERhdGEsIGluZGV4T2Zmc2V0OiB6bEluZGV4fSk7XG4gICAgICAgIH1cblxuICAgICAgICBid2cucmVhZENocm9tVHJlZShmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIGJ3Zy5nZXRBdXRvU1FMKGZ1bmN0aW9uKGFzKSB7XG4gICAgICAgICAgICAgICAgYndnLnNjaGVtYSA9IGFzO1xuICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhid2cpO1xuICAgICAgICAgICAgfSk7XG4gICAgICAgIH0pO1xuICAgIH0pO1xufVxuXG5cbkJpZ1dpZy5wcm90b3R5cGUuX3RzRmV0Y2ggPSBmdW5jdGlvbih6b29tLCBjaHIsIG1pbiwgbWF4LCBjYWxsYmFjaykge1xuICAgIHZhciBid2cgPSB0aGlzO1xuICAgIGlmICh6b29tID49IHRoaXMuem9vbUxldmVscy5sZW5ndGggLSAxKSB7XG4gICAgICAgIGlmICghdGhpcy50b3BMZXZlbFJlZHVjdGlvbkNhY2hlKSB7XG4gICAgICAgICAgICB0aGlzLmdldFpvb21lZFZpZXcodGhpcy56b29tTGV2ZWxzLmxlbmd0aCAtIDEpLnJlYWRXaWdEYXRhQnlJZCgtMSwgMCwgMzAwMDAwMDAwLCBmdW5jdGlvbihmZWF0cykge1xuICAgICAgICAgICAgICAgIGJ3Zy50b3BMZXZlbFJlZHVjdGlvbkNhY2hlID0gZmVhdHM7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGJ3Zy5fdHNGZXRjaCh6b29tLCBjaHIsIG1pbiwgbWF4LCBjYWxsYmFjayk7XG4gICAgICAgICAgICB9KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZhciBmID0gW107XG4gICAgICAgICAgICB2YXIgYyA9IHRoaXMudG9wTGV2ZWxSZWR1Y3Rpb25DYWNoZTtcbiAgICAgICAgICAgIGZvciAodmFyIGZpID0gMDsgZmkgPCBjLmxlbmd0aDsgKytmaSkge1xuICAgICAgICAgICAgICAgIGlmIChjW2ZpXS5fY2hyb21JZCA9PSBjaHIpIHtcbiAgICAgICAgICAgICAgICAgICAgZi5wdXNoKGNbZmldKTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soZik7XG4gICAgICAgIH1cbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgdmlldztcbiAgICAgICAgaWYgKHpvb20gPCAwKSB7XG4gICAgICAgICAgICB2aWV3ID0gdGhpcy5nZXRVbnpvb21lZFZpZXcoKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZpZXcgPSB0aGlzLmdldFpvb21lZFZpZXcoem9vbSk7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIHZpZXcucmVhZFdpZ0RhdGFCeUlkKGNociwgbWluLCBtYXgsIGNhbGxiYWNrKTtcbiAgICB9XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUudGhyZXNob2xkU2VhcmNoID0gZnVuY3Rpb24oY2hyTmFtZSwgcmVmZXJlbmNlUG9pbnQsIGRpciwgdGhyZXNob2xkLCBjYWxsYmFjaykge1xuICAgIGRpciA9IChkaXI8MCkgPyAtMSA6IDE7XG4gICAgdmFyIGJ3ZyA9IHRoaXM7XG4gICAgdmFyIGluaXRpYWxDaHIgPSB0aGlzLmNocm9tc1RvSURzW2Nock5hbWVdO1xuICAgIHZhciBjYW5kaWRhdGVzID0gW3tjaHJPcmQ6IDAsIGNocjogaW5pdGlhbENociwgem9vbTogYndnLnpvb21MZXZlbHMubGVuZ3RoIC0gNCwgbWluOiAwLCBtYXg6IDMwMDAwMDAwMCwgZnJvbVJlZjogdHJ1ZX1dXG4gICAgZm9yICh2YXIgaSA9IDE7IGkgPD0gdGhpcy5tYXhJRCArIDE7ICsraSkge1xuICAgICAgICB2YXIgY2hySWQgPSAoaW5pdGlhbENociArIChkaXIqaSkpICUgKHRoaXMubWF4SUQgKyAxKTtcbiAgICAgICAgaWYgKGNocklkIDwgMCkgXG4gICAgICAgICAgICBjaHJJZCArPSAodGhpcy5tYXhJRCArIDEpO1xuICAgICAgICBjYW5kaWRhdGVzLnB1c2goe2Nock9yZDogaSwgY2hyOiBjaHJJZCwgem9vbTogYndnLnpvb21MZXZlbHMubGVuZ3RoIC0gMSwgbWluOiAwLCBtYXg6IDMwMDAwMDAwMH0pXG4gICAgfVxuICAgICAgIFxuICAgIGZ1bmN0aW9uIGZiVGhyZXNob2xkU2VhcmNoUmVjdXIoKSB7XG4gICAgXHRpZiAoY2FuZGlkYXRlcy5sZW5ndGggPT0gMCkge1xuICAgIFx0ICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcbiAgICBcdH1cbiAgICBcdGNhbmRpZGF0ZXMuc29ydChmdW5jdGlvbihjMSwgYzIpIHtcbiAgICBcdCAgICB2YXIgZCA9IGMxLnpvb20gLSBjMi56b29tO1xuICAgIFx0ICAgIGlmIChkICE9IDApXG4gICAgXHRcdCAgICByZXR1cm4gZDtcblxuICAgICAgICAgICAgZCA9IGMxLmNock9yZCAtIGMyLmNock9yZDtcbiAgICAgICAgICAgIGlmIChkICE9IDApXG4gICAgICAgICAgICAgICAgcmV0dXJuIGQ7XG4gICAgXHQgICAgZWxzZVxuICAgIFx0XHQgICAgcmV0dXJuIGMxLm1pbiAtIGMyLm1pbiAqIGRpcjtcbiAgICBcdH0pO1xuXG5cdCAgICB2YXIgY2FuZGlkYXRlID0gY2FuZGlkYXRlcy5zcGxpY2UoMCwgMSlbMF07XG4gICAgICAgIGJ3Zy5fdHNGZXRjaChjYW5kaWRhdGUuem9vbSwgY2FuZGlkYXRlLmNociwgY2FuZGlkYXRlLm1pbiwgY2FuZGlkYXRlLm1heCwgZnVuY3Rpb24oZmVhdHMpIHtcbiAgICAgICAgICAgIHZhciBycCA9IGRpciA+IDAgPyAwIDogMzAwMDAwMDAwO1xuICAgICAgICAgICAgaWYgKGNhbmRpZGF0ZS5mcm9tUmVmKVxuICAgICAgICAgICAgICAgIHJwID0gcmVmZXJlbmNlUG9pbnQ7XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGZvciAodmFyIGZpID0gMDsgZmkgPCBmZWF0cy5sZW5ndGg7ICsrZmkpIHtcbiAgICBcdCAgICAgICAgdmFyIGYgPSBmZWF0c1tmaV07XG4gICAgICAgICAgICAgICAgdmFyIHNjb3JlO1xuICAgICAgICAgICAgICAgIGlmIChmLm1heFNjb3JlICE9IHVuZGVmaW5lZClcbiAgICAgICAgICAgICAgICAgICAgc2NvcmUgPSBmLm1heFNjb3JlO1xuICAgICAgICAgICAgICAgIGVsc2VcbiAgICAgICAgICAgICAgICAgICAgc2NvcmUgPSBmLnNjb3JlO1xuXG4gICAgICAgICAgICAgICAgaWYgKGRpciA+IDApIHtcbiAgICBcdCAgICAgICAgICAgIGlmIChzY29yZSA+IHRocmVzaG9sZCkge1xuICAgICAgICBcdFx0ICAgICAgICBpZiAoY2FuZGlkYXRlLnpvb20gPCAwKSB7XG4gICAgICAgIFx0XHQgICAgICAgICAgICBpZiAoZi5taW4gPiBycClcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKGYpO1xuICAgICAgICBcdFx0ICAgICAgICB9IGVsc2UgaWYgKGYubWF4ID4gcnApIHtcbiAgICAgICAgXHRcdCAgICAgICAgICAgIGNhbmRpZGF0ZXMucHVzaCh7Y2hyOiBjYW5kaWRhdGUuY2hyLCBjaHJPcmQ6IGNhbmRpZGF0ZS5jaHJPcmQsIHpvb206IGNhbmRpZGF0ZS56b29tIC0gMiwgbWluOiBmLm1pbiwgbWF4OiBmLm1heCwgZnJvbVJlZjogY2FuZGlkYXRlLmZyb21SZWZ9KTtcbiAgICAgICAgXHRcdCAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHNjb3JlID4gdGhyZXNob2xkKSB7XG4gICAgICAgICAgICBcdFx0ICAgIGlmIChjYW5kaWRhdGUuem9vbSA8IDApIHtcbiAgICAgICAgICAgICAgICBcdCAgICAgICAgaWYgKGYubWF4IDwgcnApXG4gICAgICAgICAgICAgICAgXHRcdFx0ICAgIHJldHVybiBjYWxsYmFjayhmKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH0gZWxzZSBpZiAoZi5taW4gPCBycCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGNhbmRpZGF0ZXMucHVzaCh7Y2hyOiBjYW5kaWRhdGUuY2hyLCBjaHJPcmQ6IGNhbmRpZGF0ZS5jaHJPcmQsIHpvb206IGNhbmRpZGF0ZS56b29tIC0gMiwgbWluOiBmLm1pbiwgbWF4OiBmLm1heCwgZnJvbVJlZjogY2FuZGlkYXRlLmZyb21SZWZ9KTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICBcdCAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgXHQgICAgfVxuICAgICAgICAgICAgZmJUaHJlc2hvbGRTZWFyY2hSZWN1cigpO1xuICAgICAgICB9KTtcbiAgICB9XG4gICAgXG4gICAgZmJUaHJlc2hvbGRTZWFyY2hSZWN1cigpO1xufVxuXG5CaWdXaWcucHJvdG90eXBlLmdldEF1dG9TUUwgPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG4gICAgaWYgKCF0aGlzLmFzT2Zmc2V0KVxuICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCk7XG5cblxuICAgIHRoaXMuZGF0YS5zbGljZSh0aGlzLmFzT2Zmc2V0LCAyMDQ4KS5mZXRjaChmdW5jdGlvbihyZXN1bHQpIHtcbiAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkocmVzdWx0KTtcbiAgICAgICAgdmFyIHMgPSAnJztcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBiYS5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgaWYgKGJhW2ldID09IDApXG4gICAgICAgICAgICAgICAgYnJlYWs7XG4gICAgICAgICAgICBzICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoYmFbaV0pO1xuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICAvKiBcbiAgICAgICAgICogUXVpY2snbidkaXJ0eSBhdHRlbXB0IHRvIHBhcnNlIGF1dG9TcWwgZm9ybWF0LlxuICAgICAgICAgKiBTZWU6IGh0dHA6Ly93d3cubGludXhqb3VybmFsLmNvbS9maWxlcy9saW51eGpvdXJuYWwuY29tL2xpbnV4am91cm5hbC9hcnRpY2xlcy8wNTkvNTk0OS81OTQ5bDIuaHRtbFxuICAgICAgICAgKi9cblxuICAgICAgICB2YXIgaGVhZGVyX3JlID0gLyhcXHcrKVxccysoXFx3KylcXHMrKFwiKFteXCJdKylcIik/XFxzK1xcKFxccyovO1xuICAgICAgICB2YXIgZmllbGRfcmUgPSAvKFtcXHdcXFtcXF1dKylcXHMrKFxcdyspXFxzKjtcXHMqKFwiKFteXCJdKylcIik/XFxzKi9nO1xuXG4gICAgICAgIHZhciBoZWFkZXJNYXRjaCA9IGhlYWRlcl9yZS5leGVjKHMpO1xuICAgICAgICBpZiAoaGVhZGVyTWF0Y2gpIHtcbiAgICAgICAgICAgIHZhciBhcyA9IHtcbiAgICAgICAgICAgICAgICBkZWNsVHlwZTogaGVhZGVyTWF0Y2hbMV0sXG4gICAgICAgICAgICAgICAgbmFtZTogaGVhZGVyTWF0Y2hbMl0sXG4gICAgICAgICAgICAgICAgY29tbWVudDogaGVhZGVyTWF0Y2hbNF0sXG5cbiAgICAgICAgICAgICAgICBmaWVsZHM6IFtdXG4gICAgICAgICAgICB9O1xuXG4gICAgICAgICAgICBzID0gcy5zdWJzdHJpbmcoaGVhZGVyTWF0Y2hbMF0pO1xuICAgICAgICAgICAgZm9yICh2YXIgbSA9IGZpZWxkX3JlLmV4ZWMocyk7IG0gIT0gbnVsbDsgbSA9IGZpZWxkX3JlLmV4ZWMocykpIHtcbiAgICAgICAgICAgICAgICBhcy5maWVsZHMucHVzaCh7dHlwZTogbVsxXSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgbmFtZTogbVsyXSxcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgY29tbWVudDogbVs0XX0pO1xuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soYXMpO1xuICAgICAgICB9XG4gICAgfSk7XG59XG5cbkJpZ1dpZy5wcm90b3R5cGUuZ2V0RXh0cmFJbmRpY2VzID0gZnVuY3Rpb24oY2FsbGJhY2spIHtcbiAgICB2YXIgdGhpc0IgPSB0aGlzO1xuICAgIGlmICh0aGlzLnZlcnNpb24gPCA0IHx8IHRoaXMuZXh0SGVhZGVyT2Zmc2V0ID09IDAgfHwgdGhpcy50eXBlICE9ICdiaWdiZWQnKSB7XG4gICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB0aGlzLmRhdGEuc2xpY2UodGhpcy5leHRIZWFkZXJPZmZzZXQsIDY0KS5mZXRjaChmdW5jdGlvbihyZXN1bHQpIHtcbiAgICAgICAgICAgIGlmICghcmVzdWx0KSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKG51bGwsIFwiQ291bGRuJ3QgZmV0Y2ggZXh0ZW5zaW9uIGhlYWRlclwiKTtcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkocmVzdWx0KTtcbiAgICAgICAgICAgIHZhciBzYSA9IG5ldyBJbnQxNkFycmF5KHJlc3VsdCk7XG4gICAgICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShyZXN1bHQpO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgZXh0SGVhZGVyU2l6ZSA9IHNhWzBdO1xuICAgICAgICAgICAgdmFyIGV4dHJhSW5kZXhDb3VudCA9IHNhWzFdO1xuICAgICAgICAgICAgdmFyIGV4dHJhSW5kZXhMaXN0T2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIDQpO1xuXG4gICAgICAgICAgICBpZiAoZXh0cmFJbmRleENvdW50ID09IDApIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCk7XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIC8vIEZJWE1FIDIwYnl0ZSByZWNvcmRzIG9ubHkgbWFrZSBzZW5zZSBmb3Igc2luZ2xlLWZpZWxkIGluZGljZXMuXG4gICAgICAgICAgICAvLyBSaWdodCBub3csIHRoZXNlIHNlZW0gdG8gYmUgdGhlIG9ubHkgdGhpbmdzIGFyb3VuZCwgYnV0IHRoZSBmb3JtYXRcbiAgICAgICAgICAgIC8vIGlzIGFjdHVhbGx5IG1vcmUgZ2VuZXJhbC5cbiAgICAgICAgICAgIHRoaXNCLmRhdGEuc2xpY2UoZXh0cmFJbmRleExpc3RPZmZzZXQsIGV4dHJhSW5kZXhDb3VudCAqIDIwKS5mZXRjaChmdW5jdGlvbihlaWwpIHtcbiAgICAgICAgICAgICAgICBpZiAoIWVpbCkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2sobnVsbCwgXCJDb3VsZG4ndCBmZXRjaCBpbmRleCBpbmZvXCIpO1xuICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KGVpbCk7XG4gICAgICAgICAgICAgICAgdmFyIHNhID0gbmV3IEludDE2QXJyYXkoZWlsKTtcbiAgICAgICAgICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShlaWwpO1xuXG4gICAgICAgICAgICAgICAgdmFyIGluZGljZXMgPSBbXTtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBpaSA9IDA7IGlpIDwgZXh0cmFJbmRleENvdW50OyArK2lpKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBlaVR5cGUgPSBzYVtpaSoxMF07XG4gICAgICAgICAgICAgICAgICAgIHZhciBlaUZpZWxkQ291bnQgPSBzYVtpaSoxMCArIDFdO1xuICAgICAgICAgICAgICAgICAgICB2YXIgZWlPZmZzZXQgPSBid2dfcmVhZE9mZnNldChiYSwgaWkqMjAgKyA0KTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIGVpRmllbGQgPSBzYVtpaSoxMCArIDhdXG4gICAgICAgICAgICAgICAgICAgIHZhciBpbmRleCA9IG5ldyBCQklFeHRyYUluZGV4KHRoaXNCLCBlaVR5cGUsIGVpRmllbGRDb3VudCwgZWlPZmZzZXQsIGVpRmllbGQpO1xuICAgICAgICAgICAgICAgICAgICBpbmRpY2VzLnB1c2goaW5kZXgpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjYWxsYmFjayhpbmRpY2VzKTtcbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIEJCSUV4dHJhSW5kZXgoYmJpLCB0eXBlLCBmaWVsZENvdW50LCBvZmZzZXQsIGZpZWxkKSB7XG4gICAgdGhpcy5iYmkgPSBiYmk7XG4gICAgdGhpcy50eXBlID0gdHlwZTtcbiAgICB0aGlzLmZpZWxkQ291bnQgPSBmaWVsZENvdW50O1xuICAgIHRoaXMub2Zmc2V0ID0gb2Zmc2V0O1xuICAgIHRoaXMuZmllbGQgPSBmaWVsZDtcbn1cblxuQkJJRXh0cmFJbmRleC5wcm90b3R5cGUubG9va3VwID0gZnVuY3Rpb24obmFtZSwgY2FsbGJhY2spIHtcbiAgICB2YXIgdGhpc0IgPSB0aGlzO1xuXG4gICAgdGhpcy5iYmkuZGF0YS5zbGljZSh0aGlzLm9mZnNldCwgMzIpLmZldGNoKGZ1bmN0aW9uKGJwdCkge1xuICAgICAgICB2YXIgYmEgPSBuZXcgVWludDhBcnJheShicHQpO1xuICAgICAgICB2YXIgc2EgPSBuZXcgSW50MTZBcnJheShicHQpO1xuICAgICAgICB2YXIgbGEgPSBuZXcgSW50MzJBcnJheShicHQpO1xuICAgICAgICB2YXIgYnB0TWFnaWMgPSBsYVswXTtcbiAgICAgICAgdmFyIGJsb2NrU2l6ZSA9IGxhWzFdO1xuICAgICAgICB2YXIga2V5U2l6ZSA9IGxhWzJdO1xuICAgICAgICB2YXIgdmFsU2l6ZSA9IGxhWzNdO1xuICAgICAgICB2YXIgaXRlbUNvdW50ID0gYndnX3JlYWRPZmZzZXQoYmEsIDE2KTtcbiAgICAgICAgdmFyIHJvb3ROb2RlT2Zmc2V0ID0gMzI7XG5cbiAgICAgICAgZnVuY3Rpb24gYnB0UmVhZE5vZGUobm9kZU9mZnNldCkge1xuICAgICAgICAgICAgdGhpc0IuYmJpLmRhdGEuc2xpY2Uobm9kZU9mZnNldCwgNCArIChibG9ja1NpemUgKiAoa2V5U2l6ZSArIHZhbFNpemUpKSkuZmV0Y2goZnVuY3Rpb24obm9kZSkge1xuICAgICAgICAgICAgICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KG5vZGUpO1xuICAgICAgICAgICAgICAgIHZhciBzYSA9IG5ldyBVaW50MTZBcnJheShub2RlKTtcbiAgICAgICAgICAgICAgICB2YXIgbGEgPSBuZXcgVWludDMyQXJyYXkobm9kZSk7XG5cbiAgICAgICAgICAgICAgICB2YXIgbm9kZVR5cGUgPSBiYVswXTtcbiAgICAgICAgICAgICAgICB2YXIgY250ID0gc2FbMV07XG5cbiAgICAgICAgICAgICAgICB2YXIgb2Zmc2V0ID0gNDtcbiAgICAgICAgICAgICAgICBpZiAobm9kZVR5cGUgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICB2YXIgbGFzdENoaWxkT2Zmc2V0ID0gbnVsbDtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgbiA9IDA7IG4gPCBjbnQ7ICsrbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGtleSA9ICcnO1xuICAgICAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIga2kgPSAwOyBraSA8IGtleVNpemU7ICsra2kpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgY2hhckNvZGUgPSBiYVtvZmZzZXQrK107XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGNoYXJDb2RlICE9IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAga2V5ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoY2hhckNvZGUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGNoaWxkT2Zmc2V0ID0gYndnX3JlYWRPZmZzZXQoYmEsIG9mZnNldCk7XG4gICAgICAgICAgICAgICAgICAgICAgICBvZmZzZXQgKz0gODtcbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKG5hbWUubG9jYWxlQ29tcGFyZShrZXkpIDwgMCAmJiBsYXN0Q2hpbGRPZmZzZXQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBicHRSZWFkTm9kZShsYXN0Q2hpbGRPZmZzZXQpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGxhc3RDaGlsZE9mZnNldCA9IGNoaWxkT2Zmc2V0O1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIGJwdFJlYWROb2RlKGxhc3RDaGlsZE9mZnNldCk7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgbiA9IDA7IG4gPCBjbnQ7ICsrbikge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGtleSA9ICcnO1xuICAgICAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIga2kgPSAwOyBraSA8IGtleVNpemU7ICsra2kpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgY2hhckNvZGUgPSBiYVtvZmZzZXQrK107XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKGNoYXJDb2RlICE9IDApIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAga2V5ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoY2hhckNvZGUpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gU3BlY2lmaWMgZm9yIEVJIGNhc2UuXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoa2V5ID09IG5hbWUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgc3RhcnQgPSBid2dfcmVhZE9mZnNldChiYSwgb2Zmc2V0KTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YXIgbGVuZ3RoID0gcmVhZEludChiYSwgb2Zmc2V0ICsgOCk7XG5cbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gdGhpc0IuYmJpLmdldFVuem9vbWVkVmlldygpLmZldGNoRmVhdHVyZXMoXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGZ1bmN0aW9uKGNociwgbWluLCBtYXgsIHRva3MpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmICh0b2tzICYmIHRva3MubGVuZ3RoID4gdGhpc0IuZmllbGQgLSAzKVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiB0b2tzW3RoaXNCLmZpZWxkIC0gM10gPT0gbmFtZTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgfSwgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIFt7b2Zmc2V0OiBzdGFydCwgc2l6ZTogbGVuZ3RofV0sIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBjYWxsYmFjayk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBvZmZzZXQgKz0gdmFsU2l6ZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soW10pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0pO1xuICAgICAgICB9XG5cbiAgICAgICAgYnB0UmVhZE5vZGUodGhpc0Iub2Zmc2V0ICsgcm9vdE5vZGVPZmZzZXQpO1xuICAgIH0pO1xufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIG1ha2VCd2c6IG1ha2VCd2csXG4gICAgICAgIEJJR19CRURfTUFHSUM6IEJJR19CRURfTUFHSUMsXG4gICAgICAgIEJJR19XSUdfTUFHSUM6IEJJR19XSUdfTUFHSUNcbiAgICB9XG59XG4iLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDExXG4vL1xuLy8gYmluLmpzIGdlbmVyYWwgYmluYXJ5IGRhdGEgc3VwcG9ydFxuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIHV0aWxzID0gcmVxdWlyZSgnLi91dGlscycpO1xuICAgIHZhciBzaGFsbG93Q29weSA9IHV0aWxzLnNoYWxsb3dDb3B5O1xuXG4gICAgdmFyIHNoYTEgPSByZXF1aXJlKCcuL3NoYTEnKTtcbiAgICB2YXIgYjY0X3NoYTEgPSBzaGExLmI2NF9zaGExO1xufVxuXG5mdW5jdGlvbiBCbG9iRmV0Y2hhYmxlKGIpIHtcbiAgICB0aGlzLmJsb2IgPSBiO1xufVxuXG5CbG9iRmV0Y2hhYmxlLnByb3RvdHlwZS5zbGljZSA9IGZ1bmN0aW9uKHN0YXJ0LCBsZW5ndGgpIHtcbiAgICB2YXIgYjtcblxuICAgIGlmICh0aGlzLmJsb2Iuc2xpY2UpIHtcbiAgICAgICAgaWYgKGxlbmd0aCkge1xuICAgICAgICAgICAgYiA9IHRoaXMuYmxvYi5zbGljZShzdGFydCwgc3RhcnQgKyBsZW5ndGgpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgYiA9IHRoaXMuYmxvYi5zbGljZShzdGFydCk7XG4gICAgICAgIH1cbiAgICB9IGVsc2Uge1xuICAgICAgICBpZiAobGVuZ3RoKSB7XG4gICAgICAgICAgICBiID0gdGhpcy5ibG9iLndlYmtpdFNsaWNlKHN0YXJ0LCBzdGFydCArIGxlbmd0aCk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBiID0gdGhpcy5ibG9iLndlYmtpdFNsaWNlKHN0YXJ0KTtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gbmV3IEJsb2JGZXRjaGFibGUoYik7XG59XG5cbkJsb2JGZXRjaGFibGUucHJvdG90eXBlLnNhbHRlZCA9IGZ1bmN0aW9uKCkge3JldHVybiB0aGlzO31cblxuaWYgKHR5cGVvZihGaWxlUmVhZGVyKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICAvLyBjb25zb2xlLmxvZygnZGVmaW5pbmcgYXN5bmMgQmxvYkZldGNoYWJsZS5mZXRjaCcpO1xuXG4gICAgQmxvYkZldGNoYWJsZS5wcm90b3R5cGUuZmV0Y2ggPSBmdW5jdGlvbihjYWxsYmFjaykge1xuICAgICAgICB2YXIgcmVhZGVyID0gbmV3IEZpbGVSZWFkZXIoKTtcbiAgICAgICAgcmVhZGVyLm9ubG9hZGVuZCA9IGZ1bmN0aW9uKGV2KSB7XG4gICAgICAgICAgICBjYWxsYmFjayhic3RyaW5nVG9CdWZmZXIocmVhZGVyLnJlc3VsdCkpO1xuICAgICAgICB9O1xuICAgICAgICByZWFkZXIucmVhZEFzQmluYXJ5U3RyaW5nKHRoaXMuYmxvYik7XG4gICAgfVxuXG59IGVsc2Uge1xuICAgIC8vIGlmIChjb25zb2xlICYmIGNvbnNvbGUubG9nKVxuICAgIC8vICAgIGNvbnNvbGUubG9nKCdkZWZpbmluZyBzeW5jIEJsb2JGZXRjaGFibGUuZmV0Y2gnKTtcblxuICAgIEJsb2JGZXRjaGFibGUucHJvdG90eXBlLmZldGNoID0gZnVuY3Rpb24oY2FsbGJhY2spIHtcbiAgICAgICAgdmFyIHJlYWRlciA9IG5ldyBGaWxlUmVhZGVyU3luYygpO1xuICAgICAgICB0cnkge1xuICAgICAgICAgICAgdmFyIHJlcyA9IHJlYWRlci5yZWFkQXNBcnJheUJ1ZmZlcih0aGlzLmJsb2IpO1xuICAgICAgICAgICAgY2FsbGJhY2socmVzKTtcbiAgICAgICAgfSBjYXRjaCAoZSkge1xuICAgICAgICAgICAgY2FsbGJhY2sobnVsbCwgZSk7XG4gICAgICAgIH1cbiAgICB9XG59XG5cbmZ1bmN0aW9uIFVSTEZldGNoYWJsZSh1cmwsIHN0YXJ0LCBlbmQsIG9wdHMpIHtcbiAgICBpZiAoIW9wdHMpIHtcbiAgICAgICAgaWYgKHR5cGVvZiBzdGFydCA9PT0gJ29iamVjdCcpIHtcbiAgICAgICAgICAgIG9wdHMgPSBzdGFydDtcbiAgICAgICAgICAgIHN0YXJ0ID0gdW5kZWZpbmVkO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgb3B0cyA9IHt9O1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgdGhpcy51cmwgPSB1cmw7XG4gICAgdGhpcy5zdGFydCA9IHN0YXJ0IHx8IDA7XG4gICAgaWYgKGVuZCkge1xuICAgICAgICB0aGlzLmVuZCA9IGVuZDtcbiAgICB9XG4gICAgdGhpcy5vcHRzID0gb3B0cztcbn1cblxuVVJMRmV0Y2hhYmxlLnByb3RvdHlwZS5zbGljZSA9IGZ1bmN0aW9uKHMsIGwpIHtcbiAgICBpZiAocyA8IDApIHtcbiAgICAgICAgdGhyb3cgJ0JhZCBzbGljZSAnICsgcztcbiAgICB9XG5cbiAgICB2YXIgbnMgPSB0aGlzLnN0YXJ0LCBuZSA9IHRoaXMuZW5kO1xuICAgIGlmIChucyAmJiBzKSB7XG4gICAgICAgIG5zID0gbnMgKyBzO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIG5zID0gcyB8fCBucztcbiAgICB9XG4gICAgaWYgKGwgJiYgbnMpIHtcbiAgICAgICAgbmUgPSBucyArIGwgLSAxO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIG5lID0gbmUgfHwgbCAtIDE7XG4gICAgfVxuICAgIHJldHVybiBuZXcgVVJMRmV0Y2hhYmxlKHRoaXMudXJsLCBucywgbmUsIHRoaXMub3B0cyk7XG59XG5cbnZhciBzZWVkPTA7XG52YXIgaXNTYWZhcmkgPSBuYXZpZ2F0b3IudXNlckFnZW50LmluZGV4T2YoJ1NhZmFyaScpID49IDAgJiYgbmF2aWdhdG9yLnVzZXJBZ2VudC5pbmRleE9mKCdDaHJvbWUnKSA8IDAgO1xuXG5VUkxGZXRjaGFibGUucHJvdG90eXBlLmZldGNoQXNUZXh0ID0gZnVuY3Rpb24oY2FsbGJhY2spIHtcbiAgICB2YXIgcmVxID0gbmV3IFhNTEh0dHBSZXF1ZXN0KCk7XG4gICAgdmFyIGxlbmd0aDtcbiAgICB2YXIgdXJsID0gdGhpcy51cmw7XG4gICAgaWYgKGlzU2FmYXJpIHx8IHRoaXMub3B0cy5zYWx0KSB7XG4gICAgICAgIHVybCA9IHVybCArICc/c2FsdD0nICsgYjY0X3NoYTEoJycgKyBEYXRlLm5vdygpICsgJywnICsgKCsrc2VlZCkpO1xuICAgIH1cbiAgICByZXEub3BlbignR0VUJywgdXJsLCB0cnVlKTtcblxuICAgIGlmICh0aGlzLmVuZCkge1xuICAgICAgICBpZiAodGhpcy5lbmQgLSB0aGlzLnN0YXJ0ID4gMTAwMDAwMDAwKSB7XG4gICAgICAgICAgICB0aHJvdyAnTW9uc3RlciBmZXRjaCEnO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5zZXRSZXF1ZXN0SGVhZGVyKCdSYW5nZScsICdieXRlcz0nICsgdGhpcy5zdGFydCArICctJyArIHRoaXMuZW5kKTtcbiAgICAgICAgbGVuZ3RoID0gdGhpcy5lbmQgLSB0aGlzLnN0YXJ0ICsgMTtcbiAgICB9XG5cbiAgICByZXEub25yZWFkeXN0YXRlY2hhbmdlID0gZnVuY3Rpb24oKSB7XG4gICAgICAgIGlmIChyZXEucmVhZHlTdGF0ZSA9PSA0KSB7XG4gICAgICAgICAgICBpZiAocmVxLnN0YXR1cyA9PSAyMDAgfHwgcmVxLnN0YXR1cyA9PSAyMDYpIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2socmVxLnJlc3BvbnNlVGV4dCk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgIH07XG4gICAgaWYgKHRoaXMub3B0cy5jcmVkZW50aWFscykge1xuICAgICAgICByZXEud2l0aENyZWRlbnRpYWxzID0gdHJ1ZTtcbiAgICB9XG4gICAgcmVxLnNlbmQoJycpO1xufVxuXG5VUkxGZXRjaGFibGUucHJvdG90eXBlLnNhbHRlZCA9IGZ1bmN0aW9uKCkge1xuICAgIHZhciBvID0gc2hhbGxvd0NvcHkodGhpcy5vcHRzKTtcbiAgICBvLnNhbHQgPSB0cnVlO1xuICAgIHJldHVybiBuZXcgVVJMRmV0Y2hhYmxlKHRoaXMudXJsLCB0aGlzLnN0YXJ0LCB0aGlzLmVuZCwgbyk7XG59XG5cblVSTEZldGNoYWJsZS5wcm90b3R5cGUuZmV0Y2ggPSBmdW5jdGlvbihjYWxsYmFjaywgYXR0ZW1wdCwgdHJ1bmNhdGVkTGVuZ3RoKSB7XG4gICAgdmFyIHRoaXNCID0gdGhpcztcblxuICAgIGF0dGVtcHQgPSBhdHRlbXB0IHx8IDE7XG4gICAgaWYgKGF0dGVtcHQgPiAzKSB7XG4gICAgICAgIHJldHVybiBjYWxsYmFjayhudWxsKTtcbiAgICB9XG5cbiAgICB2YXIgcmVxID0gbmV3IFhNTEh0dHBSZXF1ZXN0KCk7XG4gICAgdmFyIGxlbmd0aDtcbiAgICB2YXIgdXJsID0gdGhpcy51cmw7XG4gICAgaWYgKGlzU2FmYXJpIHx8IHRoaXMub3B0cy5zYWx0KSB7XG4gICAgICAgIHVybCA9IHVybCArICc/c2FsdD0nICsgYjY0X3NoYTEoJycgKyBEYXRlLm5vdygpICsgJywnICsgKCsrc2VlZCkpO1xuICAgIH1cbiAgICByZXEub3BlbignR0VUJywgdXJsLCB0cnVlKTtcbiAgICByZXEub3ZlcnJpZGVNaW1lVHlwZSgndGV4dC9wbGFpbjsgY2hhcnNldD14LXVzZXItZGVmaW5lZCcpO1xuICAgIGlmICh0aGlzLmVuZCkge1xuICAgICAgICBpZiAodGhpcy5lbmQgLSB0aGlzLnN0YXJ0ID4gMTAwMDAwMDAwKSB7XG4gICAgICAgICAgICB0aHJvdyAnTW9uc3RlciBmZXRjaCEnO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5zZXRSZXF1ZXN0SGVhZGVyKCdSYW5nZScsICdieXRlcz0nICsgdGhpcy5zdGFydCArICctJyArIHRoaXMuZW5kKTtcbiAgICAgICAgbGVuZ3RoID0gdGhpcy5lbmQgLSB0aGlzLnN0YXJ0ICsgMTtcbiAgICB9XG4gICAgcmVxLnJlc3BvbnNlVHlwZSA9ICdhcnJheWJ1ZmZlcic7XG4gICAgcmVxLm9ucmVhZHlzdGF0ZWNoYW5nZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICBpZiAocmVxLnJlYWR5U3RhdGUgPT0gNCkge1xuICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPT0gMjAwIHx8IHJlcS5zdGF0dXMgPT0gMjA2KSB7XG4gICAgICAgICAgICAgICAgaWYgKHJlcS5yZXNwb25zZSkge1xuICAgICAgICAgICAgICAgICAgICB2YXIgYmwgPSByZXEucmVzcG9uc2UuYnl0ZUxlbmd0aDtcbiAgICAgICAgICAgICAgICAgICAgaWYgKGxlbmd0aCAmJiBsZW5ndGggIT0gYmwgJiYgKCF0cnVuY2F0ZWRMZW5ndGggfHwgYmwgIT0gdHJ1bmNhdGVkTGVuZ3RoKSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIHRoaXNCLmZldGNoKGNhbGxiYWNrLCBhdHRlbXB0ICsgMSwgYmwpO1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKHJlcS5yZXNwb25zZSk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKHJlcS5tb3pSZXNwb25zZUFycmF5QnVmZmVyKSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBjYWxsYmFjayhyZXEubW96UmVzcG9uc2VBcnJheUJ1ZmZlcik7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHIgPSByZXEucmVzcG9uc2VUZXh0O1xuICAgICAgICAgICAgICAgICAgICBpZiAobGVuZ3RoICYmIGxlbmd0aCAhPSByLmxlbmd0aCAmJiAoIXRydW5jYXRlZExlbmd0aCB8fCByLmxlbmd0aCAhPSB0cnVuY2F0ZWRMZW5ndGgpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gdGhpc0IuZmV0Y2goY2FsbGJhY2ssIGF0dGVtcHQgKyAxLCByLmxlbmd0aCk7XG4gICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gY2FsbGJhY2soYnN0cmluZ1RvQnVmZmVyKHJlcS5yZXNwb25zZVRleHQpKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIHRoaXNCLmZldGNoKGNhbGxiYWNrLCBhdHRlbXB0ICsgMSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9O1xuICAgIGlmICh0aGlzLm9wdHMuY3JlZGVudGlhbHMpIHtcbiAgICAgICAgcmVxLndpdGhDcmVkZW50aWFscyA9IHRydWU7XG4gICAgfVxuICAgIHJlcS5zZW5kKCcnKTtcbn1cblxuZnVuY3Rpb24gYnN0cmluZ1RvQnVmZmVyKHJlc3VsdCkge1xuICAgIGlmICghcmVzdWx0KSB7XG4gICAgICAgIHJldHVybiBudWxsO1xuICAgIH1cblxuICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KHJlc3VsdC5sZW5ndGgpO1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgYmEubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgYmFbaV0gPSByZXN1bHQuY2hhckNvZGVBdChpKTtcbiAgICB9XG4gICAgcmV0dXJuIGJhLmJ1ZmZlcjtcbn1cblxuLy8gUmVhZCBmcm9tIFVpbnQ4QXJyYXlcblxuKGZ1bmN0aW9uKGdsb2JhbCkge1xuICAgIHZhciBjb252ZXJ0QnVmZmVyID0gbmV3IEFycmF5QnVmZmVyKDgpO1xuICAgIHZhciBiYSA9IG5ldyBVaW50OEFycmF5KGNvbnZlcnRCdWZmZXIpO1xuICAgIHZhciBmYSA9IG5ldyBGbG9hdDMyQXJyYXkoY29udmVydEJ1ZmZlcik7XG5cblxuICAgIGdsb2JhbC5yZWFkRmxvYXQgPSBmdW5jdGlvbihidWYsIG9mZnNldCkge1xuICAgICAgICBiYVswXSA9IGJ1ZltvZmZzZXRdO1xuICAgICAgICBiYVsxXSA9IGJ1ZltvZmZzZXQrMV07XG4gICAgICAgIGJhWzJdID0gYnVmW29mZnNldCsyXTtcbiAgICAgICAgYmFbM10gPSBidWZbb2Zmc2V0KzNdO1xuICAgICAgICByZXR1cm4gZmFbMF07XG4gICAgfTtcbiB9KHRoaXMpKTtcblxuZnVuY3Rpb24gcmVhZEludDY0KGJhLCBvZmZzZXQpIHtcbiAgICByZXR1cm4gKGJhW29mZnNldCArIDddIDw8IDI0KSB8IChiYVtvZmZzZXQgKyA2XSA8PCAxNikgfCAoYmFbb2Zmc2V0ICsgNV0gPDwgOCkgfCAoYmFbb2Zmc2V0ICsgNF0pO1xufVxuXG5mdW5jdGlvbiByZWFkSW50KGJhLCBvZmZzZXQpIHtcbiAgICByZXR1cm4gKGJhW29mZnNldCArIDNdIDw8IDI0KSB8IChiYVtvZmZzZXQgKyAyXSA8PCAxNikgfCAoYmFbb2Zmc2V0ICsgMV0gPDwgOCkgfCAoYmFbb2Zmc2V0XSk7XG59XG5cbmZ1bmN0aW9uIHJlYWRTaG9ydChiYSwgb2Zmc2V0KSB7XG4gICAgcmV0dXJuIChiYVtvZmZzZXQgKyAxXSA8PCA4KSB8IChiYVtvZmZzZXRdKTtcbn1cblxuZnVuY3Rpb24gcmVhZEJ5dGUoYmEsIG9mZnNldCkge1xuICAgIHJldHVybiBiYVtvZmZzZXRdO1xufVxuXG5mdW5jdGlvbiByZWFkSW50QkUoYmEsIG9mZnNldCkge1xuICAgIHJldHVybiAoYmFbb2Zmc2V0XSA8PCAyNCkgfCAoYmFbb2Zmc2V0ICsgMV0gPDwgMTYpIHwgKGJhW29mZnNldCArIDJdIDw8IDgpIHwgKGJhW29mZnNldCArIDNdKTtcbn1cblxuLy8gRXhwb3J0cyBpZiB3ZSBhcmUgYmVpbmcgdXNlZCBhcyBhIG1vZHVsZVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIEJsb2JGZXRjaGFibGU6IEJsb2JGZXRjaGFibGUsXG4gICAgICAgIFVSTEZldGNoYWJsZTogVVJMRmV0Y2hhYmxlLFxuXG4gICAgICAgIHJlYWRJbnQ6IHJlYWRJbnQsXG4gICAgICAgIHJlYWRJbnRCRTogcmVhZEludEJFLFxuICAgICAgICByZWFkSW50NjQ6IHJlYWRJbnQ2NCxcbiAgICAgICAgcmVhZFNob3J0OiByZWFkU2hvcnQsXG4gICAgICAgIHJlYWRCeXRlOiByZWFkQnl0ZSxcbiAgICAgICAgcmVhZEZsb2F0OiB0aGlzLnJlYWRGbG9hdFxuICAgIH1cbn1cbiIsIi8qIC0qLSBtb2RlOiBqYXZhc2NyaXB0OyBjLWJhc2ljLW9mZnNldDogNDsgaW5kZW50LXRhYnMtbW9kZTogbmlsIC0qLSAqL1xuXG4vLyBcbi8vIERhbGxpYW5jZSBHZW5vbWUgRXhwbG9yZXJcbi8vIChjKSBUaG9tYXMgRG93biAyMDA2LTIwMTBcbi8vXG4vLyBjb2xvci5qc1xuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbmZ1bmN0aW9uIERDb2xvdXIocmVkLCBncmVlbiwgYmx1ZSwgbmFtZSkge1xuICAgIHRoaXMucmVkID0gcmVkfDA7XG4gICAgdGhpcy5ncmVlbiA9IGdyZWVufDA7XG4gICAgdGhpcy5ibHVlID0gYmx1ZXwwO1xuICAgIGlmIChuYW1lKSB7XG4gICAgICAgIHRoaXMubmFtZSA9IG5hbWU7XG4gICAgfVxufVxuXG5EQ29sb3VyLnByb3RvdHlwZS50b1N2Z1N0cmluZyA9IGZ1bmN0aW9uKCkge1xuICAgIGlmICghdGhpcy5uYW1lKSB7XG4gICAgICAgIHRoaXMubmFtZSA9IFwicmdiKFwiICsgdGhpcy5yZWQgKyBcIixcIiArIHRoaXMuZ3JlZW4gKyBcIixcIiArIHRoaXMuYmx1ZSArIFwiKVwiO1xuICAgIH1cblxuICAgIHJldHVybiB0aGlzLm5hbWU7XG59XG5cbmZ1bmN0aW9uIGhleDIoeCkge1xuICAgIHZhciB5ID0gJzAwJyArIHgudG9TdHJpbmcoMTYpO1xuICAgIHJldHVybiB5LnN1YnN0cmluZyh5Lmxlbmd0aCAtIDIpO1xufVxuXG5EQ29sb3VyLnByb3RvdHlwZS50b0hleFN0cmluZyA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiAnIycgKyBoZXgyKHRoaXMucmVkKSArIGhleDIodGhpcy5ncmVlbikgKyBoZXgyKHRoaXMuYmx1ZSk7XG59XG5cbnZhciBwYWxldHRlID0ge1xuICAgIHJlZDogbmV3IERDb2xvdXIoMjU1LCAwLCAwLCAncmVkJyksXG4gICAgZ3JlZW46IG5ldyBEQ29sb3VyKDAsIDI1NSwgMCwgJ2dyZWVuJyksXG4gICAgYmx1ZTogbmV3IERDb2xvdXIoMCwgMCwgMjU1LCAnYmx1ZScpLFxuICAgIHllbGxvdzogbmV3IERDb2xvdXIoMjU1LCAyNTUsIDAsICd5ZWxsb3cnKSxcbiAgICB3aGl0ZTogbmV3IERDb2xvdXIoMjU1LCAyNTUsIDI1NSwgJ3doaXRlJyksXG4gICAgYmxhY2s6IG5ldyBEQ29sb3VyKDAsIDAsIDAsICdibGFjaycpLFxuICAgIGdyYXk6IG5ldyBEQ29sb3VyKDE4MCwgMTgwLCAxODAsICdncmF5JyksXG4gICAgZ3JleTogbmV3IERDb2xvdXIoMTgwLCAxODAsIDE4MCwgJ2dyZXknKSxcbiAgICBsaWdodHNreWJsdWU6IG5ldyBEQ29sb3VyKDEzNSwgMjA2LCAyNTAsICdsaWdodHNreWJsdWUnKSxcbiAgICBsaWdodHNhbG1vbjogbmV3IERDb2xvdXIoMjU1LCAxNjAsIDEyMiwgJ2xpZ2h0c2FsbW9uJyksXG4gICAgaG90cGluazogbmV3IERDb2xvdXIoMjU1LCAxMDUsIDE4MCwgJ2hvdHBpbmsnKVxufTtcblxudmFyIENPTE9SX1JFID0gbmV3IFJlZ0V4cCgnXiMoWzAtOUEtRmEtZl17Mn0pKFswLTlBLUZhLWZdezJ9KShbMC05QS1GYS1mXXsyfSkkJyk7XG52YXIgQ1NTX0NPTE9SX1JFID0gL3JnYlxcKChbMC05XSspLChbMC05XSspLChbMC05XSspXFwpL1xuXG5mdW5jdGlvbiBkYXNDb2xvdXJGb3JOYW1lKG5hbWUpIHtcbiAgICB2YXIgYyA9IHBhbGV0dGVbbmFtZV07XG4gICAgaWYgKCFjKSB7XG4gICAgICAgIHZhciBtYXRjaCA9IENPTE9SX1JFLmV4ZWMobmFtZSk7XG4gICAgICAgIGlmIChtYXRjaCkge1xuICAgICAgICAgICAgYyA9IG5ldyBEQ29sb3VyKCgnMHgnICsgbWF0Y2hbMV0pfDAsICgnMHgnICsgbWF0Y2hbMl0pfDAsICgnMHgnICsgbWF0Y2hbM10pfDAsIG5hbWUpO1xuICAgICAgICAgICAgcGFsZXR0ZVtuYW1lXSA9IGM7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgXHQgICAgbWF0Y2ggPSBDU1NfQ09MT1JfUkUuZXhlYyhuYW1lKTtcbiAgICBcdCAgICBpZiAobWF0Y2gpIHtcbiAgICAgICAgXHRcdGMgPSBuZXcgRENvbG91cihtYXRjaFsxXXwwLCBtYXRjaFsyXXwwLCBtYXRjaFszXXwwLCBuYW1lKTtcbiAgICAgICAgXHRcdHBhbGV0dGVbbmFtZV0gPSBjO1xuXHQgICAgICAgfSBlbHNlIHtcblx0XHQgICAgICBjb25zb2xlLmxvZyhcImNvdWxkbid0IGhhbmRsZSBjb2xvcjogXCIgKyBuYW1lKTtcblx0XHQgICAgICBjID0gcGFsZXR0ZS5ibGFjaztcblx0XHQgICAgICBwYWxldHRlW25hbWVdID0gYztcblx0ICAgICAgIH1cbiAgICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gYztcbn1cblxuZnVuY3Rpb24gbWFrZUNvbG91clN0ZXBzKHN0ZXBzLCBzdG9wcywgY29sb3Vycykge1xuICAgIHZhciBkY29sb3VycyA9IFtdO1xuICAgIGZvciAodmFyIGNpID0gMDsgY2kgPCBjb2xvdXJzLmxlbmd0aDsgKytjaSkge1xuICAgICAgICBkY29sb3Vycy5wdXNoKGRhc0NvbG91ckZvck5hbWUoY29sb3Vyc1tjaV0pKTtcbiAgICB9XG5cbiAgICB2YXIgZ3JhZCA9IFtdO1xuICBTVEVQX0xPT1A6XG4gICAgZm9yICh2YXIgc2kgPSAwOyBzaSA8IHN0ZXBzOyArK3NpKSB7XG4gICAgICAgIHZhciBycyA9ICgxLjAgKiBzaSkgLyAoc3RlcHMtMSk7XG4gICAgICAgIHZhciBzY29yZSA9IHN0b3BzWzBdICsgKHN0b3BzW3N0b3BzLmxlbmd0aCAtMV0gLSBzdG9wc1swXSkgKiBycztcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBzdG9wcy5sZW5ndGggLSAxOyArK2kpIHtcbiAgICAgICAgICAgIGlmIChzY29yZSA+PSBzdG9wc1tpXSAmJiBzY29yZSA8PSBzdG9wc1tpKzFdKSB7XG4gICAgICAgICAgICAgICAgdmFyIGZyYWMgPSAoc2NvcmUgLSBzdG9wc1tpXSkgLyAoc3RvcHNbaSsxXSAtIHN0b3BzW2ldKTtcbiAgICAgICAgICAgICAgICB2YXIgY2EgPSBkY29sb3Vyc1tpXTtcbiAgICAgICAgICAgICAgICB2YXIgY2IgPSBkY29sb3Vyc1tpKzFdO1xuXG4gICAgICAgICAgICAgICAgdmFyIGZpbGwgPSBuZXcgRENvbG91cihcbiAgICAgICAgICAgICAgICAgICAgKChjYS5yZWQgKiAoMS4wIC0gZnJhYykpICsgKGNiLnJlZCAqIGZyYWMpKXwwLFxuICAgICAgICAgICAgICAgICAgICAoKGNhLmdyZWVuICogKDEuMCAtIGZyYWMpKSArIChjYi5ncmVlbiAqIGZyYWMpKXwwLFxuICAgICAgICAgICAgICAgICAgICAoKGNhLmJsdWUgKiAoMS4wIC0gZnJhYykpICsgKGNiLmJsdWUgKiBmcmFjKSl8MFxuICAgICAgICAgICAgICAgICkudG9TdmdTdHJpbmcoKTtcbiAgICAgICAgICAgICAgICBncmFkLnB1c2goZmlsbCk7XG5cbiAgICAgICAgICAgICAgICBjb250aW51ZSBTVEVQX0xPT1A7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgdGhyb3cgJ0JhZCBzdGVwJztcbiAgICB9XG5cbiAgICByZXR1cm4gZ3JhZDtcbn1cblxuZnVuY3Rpb24gbWFrZUdyYWRpZW50KHN0ZXBzLCBjb2xvcjEsIGNvbG9yMiwgY29sb3IzKSB7XG4gICAgaWYgKGNvbG9yMykge1xuICAgICAgICByZXR1cm4gbWFrZUNvbG91clN0ZXBzKHN0ZXBzLCBbMCwgMC41LCAxXSwgW2NvbG9yMSwgY29sb3IyLCBjb2xvcjNdKTtcbiAgICB9IGVsc2Uge1xuICAgICAgICByZXR1cm4gbWFrZUNvbG91clN0ZXBzKHN0ZXBzLCBbMCwgMV0sIFtjb2xvcjEsIGNvbG9yMl0pO1xuICAgIH1cbn1cblxuaWYgKHR5cGVvZihtb2R1bGUpICE9PSAndW5kZWZpbmVkJykge1xuICAgIG1vZHVsZS5leHBvcnRzID0ge1xuICAgICAgICBtYWtlQ29sb3VyU3RlcHM6IG1ha2VDb2xvdXJTdGVwcyxcbiAgICAgICAgbWFrZUdyYWRpZW50OiBtYWtlR3JhZGllbnQsXG4gICAgICAgIGRhc0NvbG91ckZvck5hbWU6IGRhc0NvbG91ckZvck5hbWVcbiAgICB9O1xufVxuIiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gRGFsbGlhbmNlIEdlbm9tZSBFeHBsb3JlclxuLy8gKGMpIFRob21hcyBEb3duIDIwMDYtMjAxMFxuLy9cbi8vIGRhcy5qczogcXVlcmllcyBhbmQgbG93LWxldmVsIGRhdGEgbW9kZWwuXG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxuaWYgKHR5cGVvZihyZXF1aXJlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcbiAgICB2YXIgdXRpbHMgPSByZXF1aXJlKCcuL3V0aWxzJyk7XG4gICAgdmFyIHNoYWxsb3dDb3B5ID0gdXRpbHMuc2hhbGxvd0NvcHk7XG4gICAgdmFyIHB1c2hvID0gdXRpbHMucHVzaG87XG5cbiAgICB2YXIgY29sb3IgPSByZXF1aXJlKCcuL2NvbG9yJyk7XG4gICAgdmFyIG1ha2VDb2xvdXJTdGVwcyA9IGNvbG9yLm1ha2VDb2xvdXJTdGVwcztcbn1cblxudmFyIGRhc0xpYkVycm9ySGFuZGxlciA9IGZ1bmN0aW9uKGVyck1zZykge1xuICAgIGFsZXJ0KGVyck1zZyk7XG59XG52YXIgZGFzTGliUmVxdWVzdFF1ZXVlID0gbmV3IEFycmF5KCk7XG5cblxuXG5mdW5jdGlvbiBEQVNTZWdtZW50KG5hbWUsIHN0YXJ0LCBlbmQsIGRlc2NyaXB0aW9uKSB7XG4gICAgdGhpcy5uYW1lID0gbmFtZTtcbiAgICB0aGlzLnN0YXJ0ID0gc3RhcnQ7XG4gICAgdGhpcy5lbmQgPSBlbmQ7XG4gICAgdGhpcy5kZXNjcmlwdGlvbiA9IGRlc2NyaXB0aW9uO1xufVxuREFTU2VnbWVudC5wcm90b3R5cGUudG9TdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5uYW1lICsgJzonICsgdGhpcy5zdGFydCArICcuLicgKyB0aGlzLmVuZDtcbn07XG5EQVNTZWdtZW50LnByb3RvdHlwZS5pc0JvdW5kZWQgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5zdGFydCAmJiB0aGlzLmVuZDtcbn1cbkRBU1NlZ21lbnQucHJvdG90eXBlLnRvREFTUXVlcnkgPSBmdW5jdGlvbigpIHtcbiAgICB2YXIgcSA9ICdzZWdtZW50PScgKyB0aGlzLm5hbWU7XG4gICAgaWYgKHRoaXMuc3RhcnQgJiYgdGhpcy5lbmQpIHtcbiAgICAgICAgcSArPSAoJzonICsgdGhpcy5zdGFydCArICcsJyArIHRoaXMuZW5kKTtcbiAgICB9XG4gICAgcmV0dXJuIHE7XG59XG5cblxuZnVuY3Rpb24gREFTU291cmNlKGExLCBhMikge1xuICAgIHZhciBvcHRpb25zO1xuICAgIGlmICh0eXBlb2YgYTEgPT0gJ3N0cmluZycpIHtcbiAgICAgICAgdGhpcy51cmkgPSBhMTtcbiAgICAgICAgb3B0aW9ucyA9IGEyIHx8IHt9O1xuICAgIH0gZWxzZSB7XG4gICAgICAgIG9wdGlvbnMgPSBhMSB8fCB7fTtcbiAgICB9XG4gICAgZm9yICh2YXIgayBpbiBvcHRpb25zKSB7XG4gICAgICAgIGlmICh0eXBlb2Yob3B0aW9uc1trXSkgIT0gJ2Z1bmN0aW9uJykge1xuICAgICAgICAgICAgdGhpc1trXSA9IG9wdGlvbnNba107XG4gICAgICAgIH1cbiAgICB9XG5cblxuICAgIGlmICghdGhpcy5jb29yZHMpIHtcbiAgICAgICAgdGhpcy5jb29yZHMgPSBbXTtcbiAgICB9XG4gICAgaWYgKCF0aGlzLnByb3BzKSB7XG4gICAgICAgIHRoaXMucHJvcHMgPSB7fTtcbiAgICB9XG5cbiAgICB0aGlzLmRhc0Jhc2VVUkkgPSB0aGlzLnVyaTtcbiAgICBpZiAodGhpcy5kYXNCYXNlVVJJICYmIHRoaXMuZGFzQmFzZVVSSS5zdWJzdHIodGhpcy51cmkubGVuZ3RoIC0gMSkgIT0gJy8nKSB7XG4gICAgICAgIHRoaXMuZGFzQmFzZVVSSSA9IHRoaXMuZGFzQmFzZVVSSSArICcvJztcbiAgICB9XG59XG5cbmZ1bmN0aW9uIERBU0Nvb3JkcygpIHtcbn1cblxuZnVuY3Rpb24gY29vcmRzTWF0Y2goYzEsIGMyKSB7XG4gICAgcmV0dXJuIGMxLnRheG9uID09IGMyLnRheG9uICYmIGMxLmF1dGggPT0gYzIuYXV0aCAmJiBjMS52ZXJzaW9uID09IGMyLnZlcnNpb247XG59XG5cbi8vXG4vLyBEQVMgMS42IGVudHJ5X3BvaW50cyBjb21tYW5kXG4vL1xuXG5EQVNTb3VyY2UucHJvdG90eXBlLmVudHJ5UG9pbnRzID0gZnVuY3Rpb24oY2FsbGJhY2spIHtcbiAgICB2YXIgZGFzVVJJID0gdGhpcy5kYXNCYXNlVVJJICsgJ2VudHJ5X3BvaW50cyc7XG4gICAgdGhpcy5kb0Nyb3NzRG9tYWluUmVxdWVzdChkYXNVUkksIGZ1bmN0aW9uKHJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICAgICAgcmV0dXJuIGNhbGxiYWNrKFtdKTtcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIHZhciBlbnRyeVBvaW50cyA9IG5ldyBBcnJheSgpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciBzZWdzID0gcmVzcG9uc2VYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1NFR01FTlQnKTtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHNlZ3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZyA9IHNlZ3NbaV07XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWdJZCA9IHNlZy5nZXRBdHRyaWJ1dGUoJ2lkJyk7XG4gICAgICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnU2l6ZSA9IHNlZy5nZXRBdHRyaWJ1dGUoJ3NpemUnKTtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ01pbiwgc2VnTWF4O1xuICAgICAgICAgICAgICAgICAgICBpZiAoc2VnU2l6ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgc2VnTWluID0gMTsgc2VnTWF4ID0gc2VnU2l6ZXwwO1xuICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgc2VnTWluID0gc2VnLmdldEF0dHJpYnV0ZSgnc3RhcnQnKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChzZWdNaW4pIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWdNaW4gfD0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIHNlZ01heCA9IHNlZy5nZXRBdHRyaWJ1dGUoJ3N0b3AnKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChzZWdNYXgpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWdNYXggfD0gMDtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnRGVzYyA9IG51bGw7XG4gICAgICAgICAgICAgICAgICAgIGlmIChzZWcuZmlyc3RDaGlsZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgc2VnRGVzYyA9IHNlZy5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBlbnRyeVBvaW50cy5wdXNoKG5ldyBEQVNTZWdtZW50KHNlZ0lkLCBzZWdNaW4sIHNlZ01heCwgc2VnRGVzYykpO1xuICAgICAgICAgICAgICAgIH0gICAgICAgICAgXG4gICAgICAgICAgICAgICBjYWxsYmFjayhlbnRyeVBvaW50cyk7XG4gICAgfSk7ICAgICAgICAgXG59XG5cbi8vXG4vLyBEQVMgMS42IHNlcXVlbmNlIGNvbW1hbmRcbi8vIERvIHdlIG5lZWQgYW4gb3B0aW9uIHRvIGZhbGwgYmFjayB0byB0aGUgZG5hIGNvbW1hbmQ/XG4vL1xuXG5mdW5jdGlvbiBEQVNTZXF1ZW5jZShuYW1lLCBzdGFydCwgZW5kLCBhbHBoYSwgc2VxKSB7XG4gICAgdGhpcy5uYW1lID0gbmFtZTtcbiAgICB0aGlzLnN0YXJ0ID0gc3RhcnQ7XG4gICAgdGhpcy5lbmQgPSBlbmQ7XG4gICAgdGhpcy5hbHBoYWJldCA9IGFscGhhO1xuICAgIHRoaXMuc2VxID0gc2VxO1xufVxuXG5EQVNTb3VyY2UucHJvdG90eXBlLnNlcXVlbmNlID0gZnVuY3Rpb24oc2VnbWVudCwgY2FsbGJhY2spIHtcbiAgICB2YXIgZGFzVVJJID0gdGhpcy5kYXNCYXNlVVJJICsgJ3NlcXVlbmNlPycgKyBzZWdtZW50LnRvREFTUXVlcnkoKTtcbiAgICB0aGlzLmRvQ3Jvc3NEb21haW5SZXF1ZXN0KGRhc1VSSSwgZnVuY3Rpb24ocmVzcG9uc2VYTUwpIHtcbiAgICAgICAgaWYgKCFyZXNwb25zZVhNTCkge1xuICAgICAgICAgICAgY2FsbGJhY2soW10pO1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIHZhciBzZXFzID0gbmV3IEFycmF5KCk7XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgdmFyIHNlZ3MgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU0VRVUVOQ0UnKTtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBpID0gMDsgaSA8IHNlZ3MubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZyA9IHNlZ3NbaV07XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWdJZCA9IHNlZy5nZXRBdHRyaWJ1dGUoJ2lkJyk7XG4gICAgICAgICAgICAgICAgICAgIHZhciBzZWdNaW4gPSBzZWcuZ2V0QXR0cmlidXRlKCdzdGFydCcpO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnTWF4ID0gc2VnLmdldEF0dHJpYnV0ZSgnc3RvcCcpO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnQWxwaGEgPSAnRE5BJztcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ1NlcSA9IG51bGw7XG4gICAgICAgICAgICAgICAgICAgIGlmIChzZWcuZmlyc3RDaGlsZCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHJhd1NlcSA9IHNlZy5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIHNlZ1NlcSA9ICcnO1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIGlkeCA9IDA7XG4gICAgICAgICAgICAgICAgICAgICAgICB3aGlsZSAodHJ1ZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBzcGFjZSA9IHJhd1NlcS5pbmRleE9mKCdcXG4nLCBpZHgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlmIChzcGFjZSA+PSAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIHNlZ1NlcSArPSByYXdTZXEuc3Vic3RyaW5nKGlkeCwgc3BhY2UpLnRvVXBwZXJDYXNlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGlkeCA9IHNwYWNlICsgMTtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBzZWdTZXEgKz0gcmF3U2VxLnN1YnN0cmluZyhpZHgpLnRvVXBwZXJDYXNlKCk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBzZXFzLnB1c2gobmV3IERBU1NlcXVlbmNlKHNlZ0lkLCBzZWdNaW4sIHNlZ01heCwgc2VnQWxwaGEsIHNlZ1NlcSkpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBjYWxsYmFjayhzZXFzKTtcbiAgICAgICAgfVxuICAgIH0pO1xufVxuXG4vL1xuLy8gREFTIDEuNiBmZWF0dXJlcyBjb21tYW5kXG4vL1xuXG5mdW5jdGlvbiBEQVNGZWF0dXJlKCkge1xufVxuXG5mdW5jdGlvbiBEQVNHcm91cChpZCkge1xuICAgIGlmIChpZClcbiAgICAgICAgdGhpcy5pZCA9IGlkO1xufVxuXG5mdW5jdGlvbiBEQVNMaW5rKGRlc2MsIHVyaSkge1xuICAgIHRoaXMuZGVzYyA9IGRlc2M7XG4gICAgdGhpcy51cmkgPSB1cmk7XG59XG5cbkRBU1NvdXJjZS5wcm90b3R5cGUuZmVhdHVyZXMgPSBmdW5jdGlvbihzZWdtZW50LCBvcHRpb25zLCBjYWxsYmFjaykge1xuICAgIG9wdGlvbnMgPSBvcHRpb25zIHx8IHt9O1xuICAgIHZhciB0aGlzQiA9IHRoaXM7XG5cbiAgICB2YXIgZGFzVVJJO1xuICAgIGlmICh0aGlzLmZlYXR1cmVzX3VyaSkge1xuICAgICAgICBkYXNVUkkgPSB0aGlzLmZlYXR1cmVzX3VyaTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgZmlsdGVycyA9IFtdO1xuXG4gICAgICAgIGlmIChzZWdtZW50KSB7XG4gICAgICAgICAgICBmaWx0ZXJzLnB1c2goc2VnbWVudC50b0RBU1F1ZXJ5KCkpO1xuICAgICAgICB9IGVsc2UgaWYgKG9wdGlvbnMuZ3JvdXApIHtcbiAgICAgICAgICAgIHZhciBnID0gb3B0aW9ucy5ncm91cDtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgZyA9PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgnZ3JvdXBfaWQ9JyArIGcpO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBnaSA9IDA7IGdpIDwgZy5sZW5ndGg7ICsrZ2kpIHtcbiAgICAgICAgICAgICAgICAgICAgZmlsdGVycy5wdXNoKCdncm91cF9pZD0nICsgZ1tnaV0pO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIGlmIChvcHRpb25zLmFkamFjZW50KSB7XG4gICAgICAgICAgICB2YXIgYWRqID0gb3B0aW9ucy5hZGphY2VudDtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgYWRqID09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICAgICAgYWRqID0gW2Fkal07XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBmb3IgKHZhciBhaSA9IDA7IGFpIDwgYWRqLmxlbmd0aDsgKythaSkge1xuICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgnYWRqYWNlbnQ9JyArIGFkalthaV0pO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKG9wdGlvbnMudHlwZSkge1xuICAgICAgICAgICAgaWYgKHR5cGVvZiBvcHRpb25zLnR5cGUgPT0gJ3N0cmluZycpIHtcbiAgICAgICAgICAgICAgICBmaWx0ZXJzLnB1c2goJ3R5cGU9JyArIG9wdGlvbnMudHlwZSk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIGZvciAodmFyIHRpID0gMDsgdGkgPCBvcHRpb25zLnR5cGUubGVuZ3RoOyArK3RpKSB7XG4gICAgICAgICAgICAgICAgICAgIGZpbHRlcnMucHVzaCgndHlwZT0nICsgb3B0aW9ucy50eXBlW3RpXSk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIFxuICAgICAgICBpZiAob3B0aW9ucy5tYXhiaW5zKSB7XG4gICAgICAgICAgICBmaWx0ZXJzLnB1c2goJ21heGJpbnM9JyArIG9wdGlvbnMubWF4Ymlucyk7XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGlmIChmaWx0ZXJzLmxlbmd0aCA+IDApIHtcbiAgICAgICAgICAgIGRhc1VSSSA9IHRoaXMuZGFzQmFzZVVSSSArICdmZWF0dXJlcz8nICsgZmlsdGVycy5qb2luKCc7Jyk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjYWxsYmFjayhbXSwgJ05vIGZpbHRlcnMgc3BlY2lmaWVkJyk7XG4gICAgICAgIH1cbiAgICB9IFxuICAgXG5cbiAgICB0aGlzLmRvQ3Jvc3NEb21haW5SZXF1ZXN0KGRhc1VSSSwgZnVuY3Rpb24ocmVzcG9uc2VYTUwsIHJlcSkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICB2YXIgbXNnO1xuICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPT0gMCkge1xuICAgICAgICAgICAgICAgIG1zZyA9ICdzZXJ2ZXIgbWF5IG5vdCBzdXBwb3J0IENPUlMnO1xuICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICBtc2cgPSAnc3RhdHVzPScgKyByZXEuc3RhdHVzO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgY2FsbGJhY2soW10sICdGYWlsZWQgcmVxdWVzdDogJyArIG1zZyk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbi8qICAgICAgaWYgKHJlcSkge1xuICAgICAgICAgICAgdmFyIGNhcHMgPSByZXEuZ2V0UmVzcG9uc2VIZWFkZXIoJ1gtREFTLUNhcGFiaWx0aWVzJyk7XG4gICAgICAgICAgICBpZiAoY2Fwcykge1xuICAgICAgICAgICAgICAgIGFsZXJ0KGNhcHMpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9ICovXG5cbiAgICAgICAgdmFyIGZlYXR1cmVzID0gbmV3IEFycmF5KCk7XG4gICAgICAgIHZhciBzZWdtZW50TWFwID0ge307XG5cbiAgICAgICAgdmFyIHNlZ3MgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU0VHTUVOVCcpO1xuICAgICAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgc2Vncy5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgICAgIHZhciBzZWdtZW50WE1MID0gc2Vnc1tzaV07XG4gICAgICAgICAgICB2YXIgc2VnbWVudElEID0gc2VnbWVudFhNTC5nZXRBdHRyaWJ1dGUoJ2lkJyk7XG4gICAgICAgICAgICBzZWdtZW50TWFwW3NlZ21lbnRJRF0gPSB7XG4gICAgICAgICAgICAgICAgbWluOiBzZWdtZW50WE1MLmdldEF0dHJpYnV0ZSgnc3RhcnQnKSxcbiAgICAgICAgICAgICAgICBtYXg6IHNlZ21lbnRYTUwuZ2V0QXR0cmlidXRlKCdzdG9wJylcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBmZWF0dXJlWE1McyA9IHNlZ21lbnRYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0ZFQVRVUkUnKTtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgZmVhdHVyZVhNTHMubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgZmVhdHVyZSA9IGZlYXR1cmVYTUxzW2ldO1xuICAgICAgICAgICAgICAgIHZhciBkYXNGZWF0dXJlID0gbmV3IERBU0ZlYXR1cmUoKTtcbiAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnNlZ21lbnQgPSBzZWdtZW50SUQ7XG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5pZCA9IGZlYXR1cmUuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubGFiZWwgPSBmZWF0dXJlLmdldEF0dHJpYnV0ZSgnbGFiZWwnKTtcblxuXG4vKlxuICAgICAgICAgICAgICAgIHZhciBjaGlsZE5vZGVzID0gZmVhdHVyZS5jaGlsZE5vZGVzO1xuICAgICAgICAgICAgICAgIGZvciAodmFyIGMgPSAwOyBjIDwgY2hpbGROb2Rlcy5sZW5ndGg7ICsrYykge1xuICAgICAgICAgICAgICAgICAgICB2YXIgY24gPSBjaGlsZE5vZGVzW2NdO1xuICAgICAgICAgICAgICAgICAgICBpZiAoY24ubm9kZVR5cGUgPT0gTm9kZS5FTEVNRU5UX05PREUpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIHZhciBrZXkgPSBjbi50YWdOYW1lO1xuICAgICAgICAgICAgICAgICAgICAgICAgLy92YXIgdmFsID0gbnVsbDtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vaWYgKGNuLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgIC8vICAgdmFsID0gY24uZmlyc3RDaGlsZC5ub2RlVmFsdWU7XG4gICAgICAgICAgICAgICAgICAgICAgICAvL31cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmVba2V5XSA9ICd4JztcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH0gKi9cblxuXG4gICAgICAgICAgICAgICAgdmFyIHNwb3MgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJTVEFSVFwiKTtcbiAgICAgICAgICAgICAgICB2YXIgZXBvcyA9IGVsZW1lbnRWYWx1ZShmZWF0dXJlLCBcIkVORFwiKTtcbiAgICAgICAgICAgICAgICBpZiAoKHNwb3N8MCkgPiAoZXBvc3wwKSkge1xuICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLm1pbiA9IGVwb3N8MDtcbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5tYXggPSBzcG9zfDA7XG4gICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5taW4gPSBzcG9zfDA7XG4gICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubWF4ID0gZXBvc3wwO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB7XG4gICAgICAgICAgICAgICAgICAgIHZhciB0ZWMgPSBmZWF0dXJlLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdUWVBFJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmICh0ZWMubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHRlID0gdGVjWzBdO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKHRlLmZpcnN0Q2hpbGQpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnR5cGUgPSB0ZS5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUudHlwZUlkID0gdGUuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS50eXBlQ3YgPSB0ZS5nZXRBdHRyaWJ1dGUoJ2N2SWQnKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnR5cGUgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJUWVBFXCIpO1xuICAgICAgICAgICAgICAgIGlmICghZGFzRmVhdHVyZS50eXBlICYmIGRhc0ZlYXR1cmUudHlwZUlkKSB7XG4gICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUudHlwZSA9IGRhc0ZlYXR1cmUudHlwZUlkOyAvLyBGSVhNRT9cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5tZXRob2QgPSBlbGVtZW50VmFsdWUoZmVhdHVyZSwgXCJNRVRIT0RcIik7XG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICB2YXIgb3JpID0gZWxlbWVudFZhbHVlKGZlYXR1cmUsIFwiT1JJRU5UQVRJT05cIik7XG4gICAgICAgICAgICAgICAgICAgIGlmICghb3JpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBvcmkgPSAnMCc7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5vcmllbnRhdGlvbiA9IG9yaTtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5zY29yZSA9IGVsZW1lbnRWYWx1ZShmZWF0dXJlLCBcIlNDT1JFXCIpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubGlua3MgPSBkYXNMaW5rc09mKGZlYXR1cmUpO1xuICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUubm90ZXMgPSBkYXNOb3Rlc09mKGZlYXR1cmUpO1xuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHZhciBncm91cHMgPSBmZWF0dXJlLmdldEVsZW1lbnRzQnlUYWdOYW1lKFwiR1JPVVBcIik7XG4gICAgICAgICAgICAgICAgZm9yICh2YXIgZ2kgID0gMDsgZ2kgPCBncm91cHMubGVuZ3RoOyArK2dpKSB7XG4gICAgICAgICAgICAgICAgICAgIHZhciBncm91cFhNTCA9IGdyb3Vwc1tnaV07XG4gICAgICAgICAgICAgICAgICAgIHZhciBkYXNHcm91cCA9IG5ldyBEQVNHcm91cCgpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC50eXBlID0gZ3JvdXBYTUwuZ2V0QXR0cmlidXRlKCd0eXBlJyk7XG4gICAgICAgICAgICAgICAgICAgIGRhc0dyb3VwLmlkID0gZ3JvdXBYTUwuZ2V0QXR0cmlidXRlKCdpZCcpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC5saW5rcyA9IGRhc0xpbmtzT2YoZ3JvdXBYTUwpO1xuICAgICAgICAgICAgICAgICAgICBkYXNHcm91cC5ub3RlcyA9IGRhc05vdGVzT2YoZ3JvdXBYTUwpO1xuICAgICAgICAgICAgICAgICAgICBpZiAoIWRhc0ZlYXR1cmUuZ3JvdXBzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3VwcyA9IG5ldyBBcnJheShkYXNHcm91cCk7XG4gICAgICAgICAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3Vwcy5wdXNoKGRhc0dyb3VwKTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgICAgIC8vIE1hZ2ljIG5vdGVzLiAgQ2hlY2sgd2l0aCBUQUQgYmVmb3JlIGNoYW5naW5nIHRoaXMuXG4gICAgICAgICAgICAgICAgaWYgKGRhc0ZlYXR1cmUubm90ZXMpIHtcbiAgICAgICAgICAgICAgICAgICAgZm9yICh2YXIgbmkgPSAwOyBuaSA8IGRhc0ZlYXR1cmUubm90ZXMubGVuZ3RoOyArK25pKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgbiA9IGRhc0ZlYXR1cmUubm90ZXNbbmldO1xuICAgICAgICAgICAgICAgICAgICAgICAgaWYgKG4uaW5kZXhPZignR2VuZW5hbWU9JykgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHZhciBnZyA9IG5ldyBEQVNHcm91cCgpO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGdnLnR5cGU9J2dlbmUnO1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIGdnLmlkID0gbi5zdWJzdHJpbmcoOSk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgaWYgKCFkYXNGZWF0dXJlLmdyb3Vwcykge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLmdyb3VwcyA9IG5ldyBBcnJheShnZyk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgZGFzRmVhdHVyZS5ncm91cHMucHVzaChnZyk7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHBlYyA9IGZlYXR1cmUuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1BBUlQnKTtcbiAgICAgICAgICAgICAgICAgICAgaWYgKHBlYy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICB2YXIgcGFydHMgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGZvciAodmFyIHBpID0gMDsgcGkgPCBwZWMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcGFydHMucHVzaChwZWNbcGldLmdldEF0dHJpYnV0ZSgnaWQnKSk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgICAgICBkYXNGZWF0dXJlLnBhcnRzID0gcGFydHM7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAge1xuICAgICAgICAgICAgICAgICAgICB2YXIgcGVjID0gZmVhdHVyZS5nZXRFbGVtZW50c0J5VGFnTmFtZSgnUEFSRU5UJyk7XG4gICAgICAgICAgICAgICAgICAgIGlmIChwZWMubGVuZ3RoID4gMCkge1xuICAgICAgICAgICAgICAgICAgICAgICAgdmFyIHBhcmVudHMgPSBbXTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGZvciAodmFyIHBpID0gMDsgcGkgPCBwZWMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgcGFyZW50cy5wdXNoKHBlY1twaV0uZ2V0QXR0cmlidXRlKCdpZCcpKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgICAgIGRhc0ZlYXR1cmUucGFyZW50cyA9IHBhcmVudHM7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgZmVhdHVyZXMucHVzaChkYXNGZWF0dXJlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICAgICAgICAgIFxuICAgICAgICBjYWxsYmFjayhmZWF0dXJlcywgdW5kZWZpbmVkLCBzZWdtZW50TWFwKTtcbiAgICB9LFxuICAgIGZ1bmN0aW9uIChlcnIpIHtcbiAgICAgICAgY2FsbGJhY2soW10sIGVycik7XG4gICAgfSk7XG59XG5cbmZ1bmN0aW9uIERBU0FsaWdubWVudCh0eXBlKSB7XG4gICAgdGhpcy50eXBlID0gdHlwZTtcbiAgICB0aGlzLm9iamVjdHMgPSB7fTtcbiAgICB0aGlzLmJsb2NrcyA9IFtdO1xufVxuXG5EQVNTb3VyY2UucHJvdG90eXBlLmFsaWdubWVudHMgPSBmdW5jdGlvbihzZWdtZW50LCBvcHRpb25zLCBjYWxsYmFjaykge1xuICAgIHZhciBkYXNVUkkgPSB0aGlzLmRhc0Jhc2VVUkkgKyAnYWxpZ25tZW50P3F1ZXJ5PScgKyBzZWdtZW50O1xuICAgIHRoaXMuZG9Dcm9zc0RvbWFpblJlcXVlc3QoZGFzVVJJLCBmdW5jdGlvbihyZXNwb25zZVhNTCkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICBjYWxsYmFjayhbXSwgJ0ZhaWxlZCByZXF1ZXN0ICcgKyBkYXNVUkkpO1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgdmFyIGFsaWdubWVudHMgPSBbXTtcbiAgICAgICAgdmFyIGFsaVhNTHMgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnYWxpZ25tZW50Jyk7XG4gICAgICAgIGZvciAodmFyIGFpID0gMDsgYWkgPCBhbGlYTUxzLmxlbmd0aDsgKythaSkge1xuICAgICAgICAgICAgdmFyIGFsaVhNTCA9IGFsaVhNTHNbYWldO1xuICAgICAgICAgICAgdmFyIGFsaSA9IG5ldyBEQVNBbGlnbm1lbnQoYWxpWE1MLmdldEF0dHJpYnV0ZSgnYWxpZ25UeXBlJykpO1xuICAgICAgICAgICAgdmFyIG9ialhNTHMgPSBhbGlYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ2FsaWduT2JqZWN0Jyk7XG4gICAgICAgICAgICBmb3IgKHZhciBvaSA9IDA7IG9pIDwgb2JqWE1Mcy5sZW5ndGg7ICsrb2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgb2JqWE1MID0gb2JqWE1Mc1tvaV07XG4gICAgICAgICAgICAgICAgdmFyIG9iaiA9IHtcbiAgICAgICAgICAgICAgICAgICAgaWQ6ICAgICAgICAgIG9ialhNTC5nZXRBdHRyaWJ1dGUoJ2ludE9iamVjdElkJyksXG4gICAgICAgICAgICAgICAgICAgIGFjY2Vzc2lvbjogICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdkYkFjY2Vzc2lvbklkJyksXG4gICAgICAgICAgICAgICAgICAgIHZlcnNpb246ICAgICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdvYmplY3RWZXJzaW9uJyksXG4gICAgICAgICAgICAgICAgICAgIGRiU291cmNlOiAgICBvYmpYTUwuZ2V0QXR0cmlidXRlKCdkYlNvdXJjZScpLFxuICAgICAgICAgICAgICAgICAgICBkYlZlcnNpb246ICAgb2JqWE1MLmdldEF0dHJpYnV0ZSgnZGJWZXJzaW9uJylcbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIGFsaS5vYmplY3RzW29iai5pZF0gPSBvYmo7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBibG9ja1hNTHMgPSBhbGlYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ2Jsb2NrJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBiaSA9IDA7IGJpIDwgYmxvY2tYTUxzLmxlbmd0aDsgKytiaSkge1xuICAgICAgICAgICAgICAgIHZhciBibG9ja1hNTCA9IGJsb2NrWE1Mc1tiaV07XG4gICAgICAgICAgICAgICAgdmFyIGJsb2NrID0ge1xuICAgICAgICAgICAgICAgICAgICBvcmRlcjogICAgICBibG9ja1hNTC5nZXRBdHRyaWJ1dGUoJ2Jsb2NrT3JkZXInKSxcbiAgICAgICAgICAgICAgICAgICAgc2VnbWVudHM6ICAgW11cbiAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgIHZhciBzZWdYTUxzID0gYmxvY2tYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ3NlZ21lbnQnKTtcbiAgICAgICAgICAgICAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgc2VnWE1Mcy5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgICAgICAgICAgICAgdmFyIHNlZ1hNTCA9IHNlZ1hNTHNbc2ldO1xuICAgICAgICAgICAgICAgICAgICB2YXIgc2VnID0ge1xuICAgICAgICAgICAgICAgICAgICAgICAgb2JqZWN0OiAgICAgIHNlZ1hNTC5nZXRBdHRyaWJ1dGUoJ2ludE9iamVjdElkJyksXG4gICAgICAgICAgICAgICAgICAgICAgICBtaW46ICAgICAgICAgc2VnWE1MLmdldEF0dHJpYnV0ZSgnc3RhcnQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIG1heDogICAgICAgICBzZWdYTUwuZ2V0QXR0cmlidXRlKCdlbmQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIHN0cmFuZDogICAgICBzZWdYTUwuZ2V0QXR0cmlidXRlKCdzdHJhbmQnKSxcbiAgICAgICAgICAgICAgICAgICAgICAgIGNpZ2FyOiAgICAgICBlbGVtZW50VmFsdWUoc2VnWE1MLCAnY2lnYXInKVxuICAgICAgICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICAgICAgICBibG9jay5zZWdtZW50cy5wdXNoKHNlZyk7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIGFsaS5ibG9ja3MucHVzaChibG9jayk7XG4gICAgICAgICAgICB9ICAgICAgIFxuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgIGFsaWdubWVudHMucHVzaChhbGkpO1xuICAgICAgICB9XG4gICAgICAgIGNhbGxiYWNrKGFsaWdubWVudHMpO1xuICAgIH0pO1xufVxuXG5cbmZ1bmN0aW9uIERBU1N0eWxlc2hlZXQoKSB7XG4vKlxuICAgIHRoaXMuaGlnaFpvb21TdHlsZXMgPSBuZXcgT2JqZWN0KCk7XG4gICAgdGhpcy5tZWRpdW1ab29tU3R5bGVzID0gbmV3IE9iamVjdCgpO1xuICAgIHRoaXMubG93Wm9vbVN0eWxlcyA9IG5ldyBPYmplY3QoKTtcbiovXG5cbiAgICB0aGlzLnN0eWxlcyA9IFtdO1xufVxuXG5EQVNTdHlsZXNoZWV0LnByb3RvdHlwZS5wdXNoU3R5bGUgPSBmdW5jdGlvbihmaWx0ZXJzLCB6b29tLCBzdHlsZSkge1xuICAgIC8qXG5cbiAgICBpZiAoIXpvb20pIHtcbiAgICAgICAgdGhpcy5oaWdoWm9vbVN0eWxlc1t0eXBlXSA9IHN0eWxlO1xuICAgICAgICB0aGlzLm1lZGl1bVpvb21TdHlsZXNbdHlwZV0gPSBzdHlsZTtcbiAgICAgICAgdGhpcy5sb3dab29tU3R5bGVzW3R5cGVdID0gc3R5bGU7XG4gICAgfSBlbHNlIGlmICh6b29tID09ICdoaWdoJykge1xuICAgICAgICB0aGlzLmhpZ2hab29tU3R5bGVzW3R5cGVdID0gc3R5bGU7XG4gICAgfSBlbHNlIGlmICh6b29tID09ICdtZWRpdW0nKSB7XG4gICAgICAgIHRoaXMubWVkaXVtWm9vbVN0eWxlc1t0eXBlXSA9IHN0eWxlO1xuICAgIH0gZWxzZSBpZiAoem9vbSA9PSAnbG93Jykge1xuICAgICAgICB0aGlzLmxvd1pvb21TdHlsZXNbdHlwZV0gPSBzdHlsZTtcbiAgICB9XG5cbiAgICAqL1xuXG4gICAgaWYgKCFmaWx0ZXJzKSB7XG4gICAgICAgIGZpbHRlcnMgPSB7dHlwZTogJ2RlZmF1bHQnfTtcbiAgICB9XG4gICAgdmFyIHN0eWxlSG9sZGVyID0gc2hhbGxvd0NvcHkoZmlsdGVycyk7XG4gICAgaWYgKHpvb20pIHtcbiAgICAgICAgc3R5bGVIb2xkZXIuem9vbSA9IHpvb207XG4gICAgfVxuICAgIHN0eWxlSG9sZGVyLnN0eWxlID0gc3R5bGU7XG4gICAgdGhpcy5zdHlsZXMucHVzaChzdHlsZUhvbGRlcik7XG59XG5cbmZ1bmN0aW9uIERBU1N0eWxlKCkge1xufVxuXG5mdW5jdGlvbiBwYXJzZUdyYWRpZW50KGdyYWQpIHtcbiAgICB2YXIgc3RlcHMgPSBncmFkLmdldEF0dHJpYnV0ZSgnc3RlcHMnKTtcbiAgICBpZiAoc3RlcHMpIHtcbiAgICAgICAgc3RlcHMgPSBzdGVwc3wwO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHN0ZXBzID0gNTA7XG4gICAgfVxuXG5cbiAgICB2YXIgc3RvcHMgPSBbXTtcbiAgICB2YXIgY29sb3JzID0gW107XG4gICAgdmFyIHNlID0gZ3JhZC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU1RPUCcpO1xuICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzZS5sZW5ndGg7ICsrc2kpIHtcbiAgICAgICAgdmFyIHN0b3AgPSBzZVtzaV07XG4gICAgICAgIHN0b3BzLnB1c2goMS4wICogc3RvcC5nZXRBdHRyaWJ1dGUoJ3Njb3JlJykpO1xuICAgICAgICBjb2xvcnMucHVzaChzdG9wLmZpcnN0Q2hpbGQubm9kZVZhbHVlKTtcbiAgICB9XG5cbiAgICByZXR1cm4gbWFrZUNvbG91clN0ZXBzKHN0ZXBzLCBzdG9wcywgY29sb3JzKTtcbn1cblxuREFTU291cmNlLnByb3RvdHlwZS5zdHlsZXNoZWV0ID0gZnVuY3Rpb24oc3VjY2Vzc0NCLCBmYWlsdXJlQ0IpIHtcbiAgICB2YXIgZGFzVVJJLCBjcmVkcyA9IHRoaXMuY3JlZGVudGlhbHM7XG4gICAgaWYgKHRoaXMuc3R5bGVzaGVldF91cmkpIHtcbiAgICAgICAgZGFzVVJJID0gdGhpcy5zdHlsZXNoZWV0X3VyaTtcbiAgICAgICAgY3JlZHMgPSBmYWxzZTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBkYXNVUkkgPSB0aGlzLmRhc0Jhc2VVUkkgKyAnc3R5bGVzaGVldCc7XG4gICAgfVxuXG4gICAgZG9Dcm9zc0RvbWFpblJlcXVlc3QoZGFzVVJJLCBmdW5jdGlvbihyZXNwb25zZVhNTCkge1xuICAgICAgICBpZiAoIXJlc3BvbnNlWE1MKSB7XG4gICAgICAgICAgICBpZiAoZmFpbHVyZUNCKSB7XG4gICAgICAgICAgICAgICAgZmFpbHVyZUNCKCk7XG4gICAgICAgICAgICB9IFxuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIHZhciBzdHlsZXNoZWV0ID0gbmV3IERBU1N0eWxlc2hlZXQoKTtcbiAgICAgICAgdmFyIHR5cGVYTUxzID0gcmVzcG9uc2VYTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ1RZUEUnKTtcbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCB0eXBlWE1Mcy5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIHR5cGVTdHlsZSA9IHR5cGVYTUxzW2ldO1xuICAgICAgICAgICAgXG4gICAgICAgICAgICB2YXIgZmlsdGVyID0ge307XG4gICAgICAgICAgICBmaWx0ZXIudHlwZSA9IHR5cGVTdHlsZS5nZXRBdHRyaWJ1dGUoJ2lkJyk7IC8vIEFtIEkgcmlnaHQgaW4gdGhpbmtpbmcgdGhhdCB0aGlzIG1ha2VzIERBU1NUWUxFIFhNTCBpbnZhbGlkPyAgVWdoLlxuICAgICAgICAgICAgZmlsdGVyLmxhYmVsID0gdHlwZVN0eWxlLmdldEF0dHJpYnV0ZSgnbGFiZWwnKTtcbiAgICAgICAgICAgIGZpbHRlci5tZXRob2QgPSB0eXBlU3R5bGUuZ2V0QXR0cmlidXRlKCdtZXRob2QnKTtcbiAgICAgICAgICAgIHZhciBnbHlwaFhNTHMgPSB0eXBlU3R5bGUuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0dMWVBIJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBnaSA9IDA7IGdpIDwgZ2x5cGhYTUxzLmxlbmd0aDsgKytnaSkge1xuICAgICAgICAgICAgICAgIHZhciBnbHlwaFhNTCA9IGdseXBoWE1Mc1tnaV07XG4gICAgICAgICAgICAgICAgdmFyIHpvb20gPSBnbHlwaFhNTC5nZXRBdHRyaWJ1dGUoJ3pvb20nKTtcbiAgICAgICAgICAgICAgICB2YXIgZ2x5cGggPSBjaGlsZEVsZW1lbnRPZihnbHlwaFhNTCk7XG4gICAgICAgICAgICAgICAgdmFyIHN0eWxlID0gbmV3IERBU1N0eWxlKCk7XG4gICAgICAgICAgICAgICAgc3R5bGUuZ2x5cGggPSBnbHlwaC5sb2NhbE5hbWU7XG4gICAgICAgICAgICAgICAgdmFyIGNoaWxkID0gZ2x5cGguZmlyc3RDaGlsZDtcbiAgICAgICAgXG4gICAgICAgICAgICAgICAgd2hpbGUgKGNoaWxkKSB7XG4gICAgICAgICAgICAgICAgICAgIGlmIChjaGlsZC5ub2RlVHlwZSA9PSBOb2RlLkVMRU1FTlRfTk9ERSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgLy8gYWxlcnQoY2hpbGQubG9jYWxOYW1lKTtcbiAgICAgICAgICAgICAgICAgICAgICAgIGlmIChjaGlsZC5sb2NhbE5hbWUgPT0gJ0JHR1JBRCcpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICBzdHlsZVtjaGlsZC5sb2NhbE5hbWVdID0gcGFyc2VHcmFkaWVudChjaGlsZCk7XG4gICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2UgeyAgICAgIFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHN0eWxlW2NoaWxkLmxvY2FsTmFtZV0gPSBjaGlsZC5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICBjaGlsZCA9IGNoaWxkLm5leHRTaWJsaW5nO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBzdHlsZXNoZWV0LnB1c2hTdHlsZShmaWx0ZXIsIHpvb20sIHN0eWxlKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBzdWNjZXNzQ0Ioc3R5bGVzaGVldCk7XG4gICAgfSwgY3JlZHMpO1xufVxuXG4vL1xuLy8gc291cmNlcyBjb21tYW5kXG4vLyBcblxuZnVuY3Rpb24gREFTUmVnaXN0cnkodXJpLCBvcHRzKVxue1xuICAgIG9wdHMgPSBvcHRzIHx8IHt9O1xuICAgIHRoaXMudXJpID0gdXJpO1xuICAgIHRoaXMub3B0cyA9IG9wdHM7ICAgXG59XG5cbkRBU1JlZ2lzdHJ5LnByb3RvdHlwZS5zb3VyY2VzID0gZnVuY3Rpb24oY2FsbGJhY2ssIGZhaWx1cmUsIG9wdHMpXG57XG4gICAgaWYgKCFvcHRzKSB7XG4gICAgICAgIG9wdHMgPSB7fTtcbiAgICB9XG5cbiAgICB2YXIgZmlsdGVycyA9IFtdO1xuICAgIGlmIChvcHRzLnRheG9uKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgnb3JnYW5pc209JyArIG9wdHMudGF4b24pO1xuICAgIH1cbiAgICBpZiAob3B0cy5hdXRoKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgnYXV0aG9yaXR5PScgKyBvcHRzLmF1dGgpO1xuICAgIH1cbiAgICBpZiAob3B0cy52ZXJzaW9uKSB7XG4gICAgICAgIGZpbHRlcnMucHVzaCgndmVyc2lvbj0nICsgb3B0cy52ZXJzaW9uKTtcbiAgICB9XG4gICAgdmFyIHF1cmkgPSB0aGlzLnVyaTtcbiAgICBpZiAoZmlsdGVycy5sZW5ndGggPiAwKSB7XG4gICAgICAgIHF1cmkgPSBxdXJpICsgJz8nICsgZmlsdGVycy5qb2luKCcmJyk7ICAgLy8gJyYnIGFzIGEgc2VwYXJhdG9yIHRvIGhhY2sgYXJvdW5kIGRhc3JlZ2lzdHJ5Lm9yZyBidWcuXG4gICAgfVxuXG4gICAgZG9Dcm9zc0RvbWFpblJlcXVlc3QocXVyaSwgZnVuY3Rpb24ocmVzcG9uc2VYTUwpIHtcbiAgICAgICAgaWYgKCFyZXNwb25zZVhNTCAmJiBmYWlsdXJlKSB7XG4gICAgICAgICAgICBmYWlsdXJlKCk7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICB2YXIgc291cmNlcyA9IFtdOyAgICAgICBcbiAgICAgICAgdmFyIHNvdXJjZVhNTHMgPSByZXNwb25zZVhNTC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnU09VUkNFJyk7XG4gICAgICAgIGZvciAodmFyIHNpID0gMDsgc2kgPCBzb3VyY2VYTUxzLmxlbmd0aDsgKytzaSkge1xuICAgICAgICAgICAgdmFyIHNvdXJjZVhNTCA9IHNvdXJjZVhNTHNbc2ldO1xuICAgICAgICAgICAgdmFyIHZlcnNpb25YTUxzID0gc291cmNlWE1MLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdWRVJTSU9OJyk7XG4gICAgICAgICAgICBpZiAodmVyc2lvblhNTHMubGVuZ3RoIDwgMSkge1xuICAgICAgICAgICAgICAgIGNvbnRpbnVlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgICAgdmFyIHZlcnNpb25YTUwgPSB2ZXJzaW9uWE1Mc1swXTtcblxuICAgICAgICAgICAgdmFyIGNvb3JkWE1McyA9IHZlcnNpb25YTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0NPT1JESU5BVEVTJyk7XG4gICAgICAgICAgICB2YXIgY29vcmRzID0gW107XG4gICAgICAgICAgICBmb3IgKHZhciBjaSA9IDA7IGNpIDwgY29vcmRYTUxzLmxlbmd0aDsgKytjaSkge1xuICAgICAgICAgICAgICAgIHZhciBjb29yZFhNTCA9IGNvb3JkWE1Mc1tjaV07XG4gICAgICAgICAgICAgICAgdmFyIGNvb3JkID0gbmV3IERBU0Nvb3JkcygpO1xuICAgICAgICAgICAgICAgIGNvb3JkLmF1dGggPSBjb29yZFhNTC5nZXRBdHRyaWJ1dGUoJ2F1dGhvcml0eScpO1xuICAgICAgICAgICAgICAgIGNvb3JkLnRheG9uID0gY29vcmRYTUwuZ2V0QXR0cmlidXRlKCd0YXhpZCcpO1xuICAgICAgICAgICAgICAgIGNvb3JkLnZlcnNpb24gPSBjb29yZFhNTC5nZXRBdHRyaWJ1dGUoJ3ZlcnNpb24nKTtcbiAgICAgICAgICAgICAgICBjb29yZHMucHVzaChjb29yZCk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIHZhciBjYXBzID0gW107XG4gICAgICAgICAgICB2YXIgY2FwWE1McyA9IHZlcnNpb25YTUwuZ2V0RWxlbWVudHNCeVRhZ05hbWUoJ0NBUEFCSUxJVFknKTtcbiAgICAgICAgICAgIHZhciB1cmk7XG4gICAgICAgICAgICBmb3IgKHZhciBjaSA9IDA7IGNpIDwgY2FwWE1Mcy5sZW5ndGg7ICsrY2kpIHtcbiAgICAgICAgICAgICAgICB2YXIgY2FwWE1MID0gY2FwWE1Mc1tjaV07XG4gICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgY2Fwcy5wdXNoKGNhcFhNTC5nZXRBdHRyaWJ1dGUoJ3R5cGUnKSk7XG5cbiAgICAgICAgICAgICAgICBpZiAoY2FwWE1MLmdldEF0dHJpYnV0ZSgndHlwZScpID09ICdkYXMxOmZlYXR1cmVzJykge1xuICAgICAgICAgICAgICAgICAgICB2YXIgZmVwID0gY2FwWE1MLmdldEF0dHJpYnV0ZSgncXVlcnlfdXJpJyk7XG4gICAgICAgICAgICAgICAgICAgIHVyaSA9IGZlcC5zdWJzdHJpbmcoMCwgZmVwLmxlbmd0aCAtICgnZmVhdHVyZXMnLmxlbmd0aCkpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgdmFyIHByb3BzID0ge307XG4gICAgICAgICAgICB2YXIgcHJvcFhNTHMgPSB2ZXJzaW9uWE1MLmdldEVsZW1lbnRzQnlUYWdOYW1lKCdQUk9QJyk7XG4gICAgICAgICAgICBmb3IgKHZhciBwaSA9IDA7IHBpIDwgcHJvcFhNTHMubGVuZ3RoOyArK3BpKSB7XG4gICAgICAgICAgICAgICAgcHVzaG8ocHJvcHMsIHByb3BYTUxzW3BpXS5nZXRBdHRyaWJ1dGUoJ25hbWUnKSwgcHJvcFhNTHNbcGldLmdldEF0dHJpYnV0ZSgndmFsdWUnKSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBcbiAgICAgICAgICAgIGlmICh1cmkpIHtcbiAgICAgICAgICAgICAgICB2YXIgc291cmNlID0gbmV3IERBU1NvdXJjZSh1cmksIHtcbiAgICAgICAgICAgICAgICAgICAgc291cmNlX3VyaTogc291cmNlWE1MLmdldEF0dHJpYnV0ZSgndXJpJyksXG4gICAgICAgICAgICAgICAgICAgIG5hbWU6ICBzb3VyY2VYTUwuZ2V0QXR0cmlidXRlKCd0aXRsZScpLFxuICAgICAgICAgICAgICAgICAgICBkZXNjOiAgc291cmNlWE1MLmdldEF0dHJpYnV0ZSgnZGVzY3JpcHRpb24nKSxcbiAgICAgICAgICAgICAgICAgICAgY29vcmRzOiBjb29yZHMsXG4gICAgICAgICAgICAgICAgICAgIHByb3BzOiBwcm9wcyxcbiAgICAgICAgICAgICAgICAgICAgY2FwYWJpbGl0aWVzOiBjYXBzXG4gICAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgICAgc291cmNlcy5wdXNoKHNvdXJjZSk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgICAgXG4gICAgICAgIGNhbGxiYWNrKHNvdXJjZXMpO1xuICAgIH0pO1xufVxuXG5cbi8vXG4vLyBVdGlsaXR5IGZ1bmN0aW9uc1xuLy9cblxuZnVuY3Rpb24gZWxlbWVudFZhbHVlKGVsZW1lbnQsIHRhZylcbntcbiAgICB2YXIgY2hpbGRyZW4gPSBlbGVtZW50LmdldEVsZW1lbnRzQnlUYWdOYW1lKHRhZyk7XG4gICAgaWYgKGNoaWxkcmVuLmxlbmd0aCA+IDAgJiYgY2hpbGRyZW5bMF0uZmlyc3RDaGlsZCkge1xuICAgICAgICB2YXIgYyA9IGNoaWxkcmVuWzBdO1xuICAgICAgICBpZiAoYy5jaGlsZE5vZGVzLmxlbmd0aCA9PSAxKSB7XG4gICAgICAgICAgICByZXR1cm4gYy5maXJzdENoaWxkLm5vZGVWYWx1ZTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHZhciBzID0gJyc7XG4gICAgICAgICAgICBmb3IgKHZhciBuaSA9IDA7IG5pIDwgYy5jaGlsZE5vZGVzLmxlbmd0aDsgKytuaSkge1xuICAgICAgICAgICAgICAgIHMgKz0gYy5jaGlsZE5vZGVzW25pXS5ub2RlVmFsdWU7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gcztcbiAgICAgICAgfVxuXG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBjaGlsZEVsZW1lbnRPZihlbGVtZW50KVxue1xuICAgIGlmIChlbGVtZW50Lmhhc0NoaWxkTm9kZXMoKSkge1xuICAgICAgICB2YXIgY2hpbGQgPSBlbGVtZW50LmZpcnN0Q2hpbGQ7XG4gICAgICAgIGRvIHtcbiAgICAgICAgICAgIGlmIChjaGlsZC5ub2RlVHlwZSA9PSBOb2RlLkVMRU1FTlRfTk9ERSkge1xuICAgICAgICAgICAgICAgIHJldHVybiBjaGlsZDtcbiAgICAgICAgICAgIH0gXG4gICAgICAgICAgICBjaGlsZCA9IGNoaWxkLm5leHRTaWJsaW5nO1xuICAgICAgICB9IHdoaWxlIChjaGlsZCAhPSBudWxsKTtcbiAgICB9XG4gICAgcmV0dXJuIG51bGw7XG59XG5cblxuZnVuY3Rpb24gZGFzTGlua3NPZihlbGVtZW50KVxue1xuICAgIHZhciBsaW5rcyA9IG5ldyBBcnJheSgpO1xuICAgIHZhciBtYXliZUxpbmtDaGlsZGVuID0gZWxlbWVudC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnTElOSycpO1xuICAgIGZvciAodmFyIGNpID0gMDsgY2kgPCBtYXliZUxpbmtDaGlsZGVuLmxlbmd0aDsgKytjaSkge1xuICAgICAgICB2YXIgbGlua1hNTCA9IG1heWJlTGlua0NoaWxkZW5bY2ldO1xuICAgICAgICBpZiAobGlua1hNTC5wYXJlbnROb2RlID09IGVsZW1lbnQpIHtcbiAgICAgICAgICAgIGxpbmtzLnB1c2gobmV3IERBU0xpbmsobGlua1hNTC5maXJzdENoaWxkID8gbGlua1hNTC5maXJzdENoaWxkLm5vZGVWYWx1ZSA6ICdVbmtub3duJywgbGlua1hNTC5nZXRBdHRyaWJ1dGUoJ2hyZWYnKSkpO1xuICAgICAgICB9XG4gICAgfVxuICAgIFxuICAgIHJldHVybiBsaW5rcztcbn1cblxuZnVuY3Rpb24gZGFzTm90ZXNPZihlbGVtZW50KVxue1xuICAgIHZhciBub3RlcyA9IFtdO1xuICAgIHZhciBtYXliZU5vdGVzID0gZWxlbWVudC5nZXRFbGVtZW50c0J5VGFnTmFtZSgnTk9URScpO1xuICAgIGZvciAodmFyIG5pID0gMDsgbmkgPCBtYXliZU5vdGVzLmxlbmd0aDsgKytuaSkge1xuICAgICAgICBpZiAobWF5YmVOb3Rlc1tuaV0uZmlyc3RDaGlsZCkge1xuICAgICAgICAgICAgbm90ZXMucHVzaChtYXliZU5vdGVzW25pXS5maXJzdENoaWxkLm5vZGVWYWx1ZSk7XG4gICAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIG5vdGVzO1xufVxuXG5mdW5jdGlvbiBkb0Nyb3NzRG9tYWluUmVxdWVzdCh1cmwsIGhhbmRsZXIsIGNyZWRlbnRpYWxzLCBjdXN0QXV0aCkge1xuICAgIC8vIFRPRE86IGV4cGxpY2l0IGVycm9yIGhhbmRsZXJzP1xuXG4gICAgaWYgKHdpbmRvdy5YRG9tYWluUmVxdWVzdCkge1xuICAgICAgICB2YXIgcmVxID0gbmV3IFhEb21haW5SZXF1ZXN0KCk7XG4gICAgICAgIHJlcS5vbmxvYWQgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgIHZhciBkb20gPSBuZXcgQWN0aXZlWE9iamVjdChcIk1pY3Jvc29mdC5YTUxET01cIik7XG4gICAgICAgICAgICBkb20uYXN5bmMgPSBmYWxzZTtcbiAgICAgICAgICAgIGRvbS5sb2FkWE1MKHJlcS5yZXNwb25zZVRleHQpO1xuICAgICAgICAgICAgaGFuZGxlcihkb20pO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5vcGVuKFwiZ2V0XCIsIHVybCk7XG4gICAgICAgIHJlcS5zZW5kKCcnKTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgcmVxU3RhcnQgPSBEYXRlLm5vdygpO1xuICAgICAgICB2YXIgcmVxID0gbmV3IFhNTEh0dHBSZXF1ZXN0KCk7XG5cbiAgICAgICAgcmVxLm9ucmVhZHlzdGF0ZWNoYW5nZSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICAgICAgaWYgKHJlcS5yZWFkeVN0YXRlID09IDQpIHtcbiAgICAgICAgICAgICAgaWYgKHJlcS5zdGF0dXMgPj0gMjAwIHx8IHJlcS5zdGF0dXMgPT0gMCkge1xuICAgICAgICAgICAgICAgICAgaGFuZGxlcihyZXEucmVzcG9uc2VYTUwsIHJlcSk7XG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgfTtcbiAgICAgICAgcmVxLm9wZW4oXCJnZXRcIiwgdXJsLCB0cnVlKTtcbiAgICAgICAgaWYgKGNyZWRlbnRpYWxzKSB7XG4gICAgICAgICAgICByZXEud2l0aENyZWRlbnRpYWxzID0gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgICBpZiAoY3VzdEF1dGgpIHtcbiAgICAgICAgICAgIHJlcS5zZXRSZXF1ZXN0SGVhZGVyKCdYLURBUy1BdXRob3Jpc2F0aW9uJywgY3VzdEF1dGgpO1xuICAgICAgICB9XG4gICAgICAgIHJlcS5vdmVycmlkZU1pbWVUeXBlKCd0ZXh0L3htbCcpO1xuICAgICAgICByZXEuc2V0UmVxdWVzdEhlYWRlcignQWNjZXB0JywgJ2FwcGxpY2F0aW9uL3htbCwqLyonKTtcbiAgICAgICAgcmVxLnNlbmQoJycpO1xuICAgIH1cbn1cblxuREFTU291cmNlLnByb3RvdHlwZS5kb0Nyb3NzRG9tYWluUmVxdWVzdCA9IGZ1bmN0aW9uKHVybCwgaGFuZGxlciwgZXJySGFuZGxlcikge1xuICAgIHZhciBjdXN0QXV0aDtcbiAgICBpZiAodGhpcy54VXNlcikge1xuICAgICAgICBjdXN0QXV0aCA9ICdCYXNpYyAnICsgYnRvYSh0aGlzLnhVc2VyICsgJzonICsgdGhpcy54UGFzcyk7XG4gICAgfVxuXG4gICAgdHJ5IHtcbiAgICAgICAgcmV0dXJuIGRvQ3Jvc3NEb21haW5SZXF1ZXN0KHVybCwgaGFuZGxlciwgdGhpcy5jcmVkZW50aWFscywgY3VzdEF1dGgpO1xuICAgIH0gY2F0Y2ggKGVycikge1xuICAgICAgICBpZiAoZXJySGFuZGxlcikge1xuICAgICAgICAgICAgZXJySGFuZGxlcihlcnIpO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgdGhyb3cgZXJyO1xuICAgICAgICB9XG4gICAgfVxufVxuXG5mdW5jdGlvbiBpc0Rhc0Jvb2xlYW5UcnVlKHMpIHtcbiAgICBzID0gKCcnICsgcykudG9Mb3dlckNhc2UoKTtcbiAgICByZXR1cm4gcz09PSd5ZXMnIHx8IHM9PT0ndHJ1ZSc7XG59XG5cbmZ1bmN0aW9uIGlzRGFzQm9vbGVhbk5vdEZhbHNlKHMpIHtcbiAgICBpZiAoIXMpXG4gICAgICAgIHJldHVybiBmYWxzZTtcblxuICAgIHMgPSAoJycgKyBzKS50b0xvd2VyQ2FzZSgpO1xuICAgIHJldHVybiBzIT09J25vJyB8fCBzIT09J2ZhbHNlJztcbn1cblxuZnVuY3Rpb24gY29weVN0eWxlc2hlZXQoc3MpIHtcbiAgICB2YXIgbnNzID0gc2hhbGxvd0NvcHkoc3MpO1xuICAgIG5zcy5zdHlsZXMgPSBbXTtcbiAgICBmb3IgKHZhciBzaSA9IDA7IHNpIDwgc3Muc3R5bGVzLmxlbmd0aDsgKytzaSkge1xuICAgICAgICB2YXIgc2ggPSBuc3Muc3R5bGVzW3NpXSA9IHNoYWxsb3dDb3B5KHNzLnN0eWxlc1tzaV0pO1xuICAgICAgICBzaC5fbWV0aG9kUkUgPSBzaC5fbGFiZWxSRSA9IHNoLl90eXBlUkUgPSB1bmRlZmluZWQ7XG4gICAgICAgIHNoLnN0eWxlID0gc2hhbGxvd0NvcHkoc2guc3R5bGUpO1xuICAgICAgICBzaC5zdHlsZS5pZCA9IHVuZGVmaW5lZDtcbiAgICAgICAgc2guc3R5bGUuX2dyYWRpZW50ID0gdW5kZWZpbmVkO1xuICAgIH1cbiAgICByZXR1cm4gbnNzO1xufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIERBU0dyb3VwOiBEQVNHcm91cCxcbiAgICAgICAgREFTRmVhdHVyZTogREFTRmVhdHVyZSxcbiAgICAgICAgREFTU3R5bGVzaGVldDogREFTU3R5bGVzaGVldCxcbiAgICAgICAgREFTU3R5bGU6IERBU1N0eWxlLFxuICAgICAgICBEQVNTb3VyY2U6IERBU1NvdXJjZSxcbiAgICAgICAgREFTU2VnbWVudDogREFTU2VnbWVudCxcbiAgICAgICAgREFTUmVnaXN0cnk6IERBU1JlZ2lzdHJ5LFxuICAgICAgICBEQVNTZXF1ZW5jZTogREFTU2VxdWVuY2UsXG4gICAgICAgIERBU0xpbms6IERBU0xpbmssXG5cbiAgICAgICAgaXNEYXNCb29sZWFuVHJ1ZTogaXNEYXNCb29sZWFuVHJ1ZSxcbiAgICAgICAgaXNEYXNCb29sZWFuTm90RmFsc2U6IGlzRGFzQm9vbGVhbk5vdEZhbHNlLFxuICAgICAgICBjb3B5U3R5bGVzaGVldDogY29weVN0eWxlc2hlZXQsXG4gICAgICAgIGNvb3Jkc01hdGNoOiBjb29yZHNNYXRjaFxuICAgIH07XG59IiwiKGZ1bmN0aW9uIChnbG9iYWwpe1xuLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gRGFsbGlhbmNlIEdlbm9tZSBFeHBsb3JlclxuLy8gKGMpIFRob21hcyBEb3duIDIwMDYtMjAxNFxuLy9cbi8vIGZldGNod29ya2VyLmpzXG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxudmFyIGJpbiA9IHJlcXVpcmUoJy4vYmluJyk7XG52YXIgYmFtID0gcmVxdWlyZSgnLi9iYW0nKTtcbnZhciBiaWd3aWcgPSByZXF1aXJlKCcuL2JpZ3dpZycpO1xuXG52YXIgY29ubmVjdGlvbnMgPSB7fTtcblxudmFyIGlkU2VlZCA9IDA7XG5cbmdsb2JhbC5uZXdJRCA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiAnY24nICsgKCsraWRTZWVkKTtcbn1cblxucG9zdE1lc3NhZ2Uoe3RhZzogJ2luaXQnfSk7XG5cbnNlbGYub25tZXNzYWdlID0gZnVuY3Rpb24oZXZlbnQpIHtcbiAgICB2YXIgZCA9IGV2ZW50LmRhdGE7XG4gICAgdmFyIGNvbW1hbmQgPSBldmVudC5kYXRhLmNvbW1hbmQ7XG4gICAgdmFyIHRhZyA9IGV2ZW50LmRhdGEudGFnO1xuXG4gICAgaWYgKGNvbW1hbmQgPT09ICdjb25uZWN0QkFNJykge1xuICAgICAgICB2YXIgaWQgPSBuZXdJRCgpO1xuXG4gICAgICAgIHZhciBiYW1GLCBiYWlGLCBpbmRleENodW5rcztcbiAgICAgICAgaWYgKGQuYmxvYikge1xuICAgICAgICAgICAgYmFtRiA9IG5ldyBiaW4uQmxvYkZldGNoYWJsZShkLmJsb2IpO1xuICAgICAgICAgICAgYmFpRiA9IG5ldyBiaW4uQmxvYkZldGNoYWJsZShkLmluZGV4QmxvYik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBiYW1GID0gbmV3IGJpbi5VUkxGZXRjaGFibGUoZC51cmksIHtjcmVkZW50aWFsczogZC5jcmVkZW50aWFsc30pO1xuICAgICAgICAgICAgYmFpRiA9IG5ldyBiaW4uVVJMRmV0Y2hhYmxlKGQuaW5kZXhVcmksIHtjcmVkZW50aWFsczogZC5jcmVkZW50aWFsc30pO1xuICAgICAgICAgICAgaW5kZXhDaHVua3MgPSBkLmluZGV4Q2h1bmtzO1xuICAgICAgICB9XG5cbiAgICAgICAgYmFtLm1ha2VCYW0oYmFtRiwgYmFpRiwgaW5kZXhDaHVua3MsIGZ1bmN0aW9uKGJhbU9iaiwgZXJyKSB7XG4gICAgICAgICAgICBpZiAoYmFtT2JqKSB7XG4gICAgICAgICAgICAgICAgY29ubmVjdGlvbnNbaWRdID0gbmV3IEJBTVdvcmtlckZldGNoZXIoYmFtT2JqKTtcbiAgICAgICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogaWR9KTtcbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCBlcnJvcjogZXJyIHx8IFwiQ291bGRuJ3QgZmV0Y2ggQkFNXCJ9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAnY29ubmVjdEJCSScpIHtcbiAgICAgICAgdmFyIGlkID0gbmV3SUQoKTtcbiAgICAgICAgdmFyIGJiaTtcbiAgICAgICAgaWYgKGQuYmxvYikge1xuICAgICAgICAgICAgYmJpID0gbmV3IGJpbi5CbG9iRmV0Y2hhYmxlKGQuYmxvYik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBiYmkgPSBuZXcgYmluLlVSTEZldGNoYWJsZShkLnVyaSwge2NyZWRlbnRpYWxzOiBkLmNyZWRlbnRpYWxzfSk7XG4gICAgICAgIH1cblxuICAgICAgICBiaWd3aWcubWFrZUJ3ZyhiYmksIGZ1bmN0aW9uKGJ3ZywgZXJyKSB7XG4gICAgICAgICAgICBpZiAoYndnKSB7XG4gICAgICAgICAgICAgICAgY29ubmVjdGlvbnNbaWRdID0gbmV3IEJCSVdvcmtlckZldGNoZXIoYndnKTtcbiAgICAgICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogaWR9KTtcbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCBlcnJvcjogZXJyIHx8IFwiQ291bGRuJ3QgZmV0Y2ggQkJJXCJ9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfSwgZC51cmkpO1xuICAgIH0gZWxzZSBpZiAoY29tbWFuZCA9PT0gJ2ZldGNoJykge1xuICAgICAgICB2YXIgY29uID0gY29ubmVjdGlvbnNbZXZlbnQuZGF0YS5jb25uZWN0aW9uXTtcbiAgICAgICAgaWYgKCFjb24pIHtcbiAgICAgICAgICAgIHJldHVybiBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnTm8gc3VjaCBjb25uZWN0aW9uOiAnICsgZXZlbnQuZGF0YS5jb25uZWN0aW9ufSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb24uZmV0Y2goZC50YWcsIGQuY2hyLCBkLm1pbiwgZC5tYXgsIGQuem9vbSwgZC5vcHRzKTtcbiAgICB9IGVsc2UgaWYgKGNvbW1hbmQgPT09ICdsZWFwJykge1xuICAgICAgICB2YXIgY29uID0gY29ubmVjdGlvbnNbZXZlbnQuZGF0YS5jb25uZWN0aW9uXTtcbiAgICAgICAgaWYgKCFjb24pIHtcbiAgICAgICAgICAgIHJldHVybiBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnTm8gc3VjaCBjb25uZWN0aW9uOiAnICsgZXZlbnQuZGF0YS5jb25uZWN0aW9ufSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb24ubGVhcChkLnRhZywgZC5jaHIsIGQucG9zLCBkLmRpcik7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAncXVhbnRMZWFwJykge1xuICAgICAgICB2YXIgY29uID0gY29ubmVjdGlvbnNbZXZlbnQuZGF0YS5jb25uZWN0aW9uXTtcbiAgICAgICAgaWYgKCFjb24pIHtcbiAgICAgICAgICAgIHJldHVybiBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnTm8gc3VjaCBjb25uZWN0aW9uOiAnICsgZXZlbnQuZGF0YS5jb25uZWN0aW9ufSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb24ucXVhbnRMZWFwKGQudGFnLCBkLmNociwgZC5wb3MsIGQuZGlyLCBkLnRocmVzaG9sZCwgZC51bmRlcik7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAnbWV0YScpIHtcbiAgICAgICAgdmFyIGNvbiA9IGNvbm5lY3Rpb25zW2V2ZW50LmRhdGEuY29ubmVjdGlvbl07XG4gICAgICAgIGlmICghY29uKSB7XG4gICAgICAgICAgICByZXR1cm4gcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCBlcnJvcjogJ05vIHN1Y2ggY29ubmVjdGlvbjogJyArIGV2ZW50LmRhdGEuY29ubmVjdGlvbn0pO1xuICAgICAgICB9XG5cbiAgICAgICAgY29uLm1ldGEoZC50YWcpO1xuICAgIH0gZWxzZSBpZiAoY29tbWFuZCA9PT0gJ3NlYXJjaCcpIHtcbiAgICAgICAgdmFyIGNvbiA9IGNvbm5lY3Rpb25zW2V2ZW50LmRhdGEuY29ubmVjdGlvbl07XG4gICAgICAgIGlmICghY29uKSB7XG4gICAgICAgICAgICByZXR1cm4gcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCBlcnJvcjogJ05vIHN1Y2ggY29ubmVjdGlvbjogJyArIGV2ZW50LmRhdGEuY29ubmVjdGlvbn0pO1xuICAgICAgICB9XG5cbiAgICAgICAgY29uLnNlYXJjaChkLnRhZywgZC5xdWVyeSwgZC5pbmRleCk7XG4gICAgfSBlbHNlIGlmIChjb21tYW5kID09PSAnZGF0ZScpIHtcbiAgICAgICAgcmV0dXJuIHBvc3RNZXNzYWdlKHt0YWc6IHRhZywgcmVzdWx0OiBEYXRlLm5vdygpfDB9KTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiAnQmFkIGNvbW1hbmQgJyArIGNvbW1hbmR9KTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIEJBTVdvcmtlckZldGNoZXIoYmFtKSB7XG4gICAgdGhpcy5iYW0gPSBiYW07XG59XG5cbkJBTVdvcmtlckZldGNoZXIucHJvdG90eXBlLmZldGNoID0gZnVuY3Rpb24odGFnLCBjaHIsIG1pbiwgbWF4LCB6b29tLCBvcHRzKSB7XG4gICAgb3B0cyA9IG9wdHMgfHwge307XG4gICAgdGhpcy5iYW0uZmV0Y2goY2hyLCBtaW4sIG1heCwgZnVuY3Rpb24ocmVjb3JkcywgZXJyKSB7XG4gICAgICAgIGlmIChyZWNvcmRzKSB7XG4gICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogcmVjb3JkcywgdGltZTogRGF0ZS5ub3coKXwwfSk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIGVycm9yOiBlcnJ9KTtcbiAgICAgICAgfVxuICAgIH0sIG9wdHMpO1xufVxuXG5mdW5jdGlvbiBCQklXb3JrZXJGZXRjaGVyKGJiaSkge1xuICAgIHRoaXMuYmJpID0gYmJpO1xufVxuXG5CQklXb3JrZXJGZXRjaGVyLnByb3RvdHlwZS5mZXRjaCA9IGZ1bmN0aW9uKHRhZywgY2hyLCBtaW4sIG1heCwgem9vbSkge1xuICAgIGlmICh0eXBlb2Yoem9vbSkgIT09ICdudW1iZXInKVxuICAgICAgICB6b29tID0gLTE7XG5cbiAgICB2YXIgZGF0YTtcbiAgICBpZiAoem9vbSA8IDApIHtcbiAgICAgICAgZGF0YSA9IHRoaXMuYmJpLmdldFVuem9vbWVkVmlldygpO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIGRhdGEgPSB0aGlzLmJiaS5nZXRab29tZWRWaWV3KHpvb20pO1xuICAgIH1cblxuICAgIGRhdGEucmVhZFdpZ0RhdGEoY2hyLCBtaW4sIG1heCwgZnVuY3Rpb24oZmVhdHVyZXMpIHtcbiAgICAgICAgcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCByZXN1bHQ6IGZlYXR1cmVzfSk7XG4gICAgfSk7XG59XG5cbkJCSVdvcmtlckZldGNoZXIucHJvdG90eXBlLm1ldGEgPSBmdW5jdGlvbih0YWcpIHtcbiAgICB2YXIgc2NhbGVzID0gWzFdO1xuICAgIGZvciAodmFyIHogPSAwOyB6IDwgdGhpcy5iYmkuem9vbUxldmVscy5sZW5ndGg7ICsreikge1xuICAgICAgICBzY2FsZXMucHVzaCh0aGlzLmJiaS56b29tTGV2ZWxzW3pdLnJlZHVjdGlvbik7XG4gICAgfVxuXG4gICAgdmFyIHRoaXNCID0gdGhpcztcbiAgICB2YXIgbWV0YSA9IHt0eXBlOiB0aGlzLmJiaS50eXBlLFxuICAgICAgICAgICAgICAgIHpvb21MZXZlbHM6IHNjYWxlcyxcbiAgICAgICAgICAgICAgICBmaWVsZENvdW50OiB0aGlzLmJiaS5maWVsZENvdW50LFxuICAgICAgICAgICAgICAgIGRlZmluZWRGaWVsZENvdW50OiB0aGlzLmJiaS5kZWZpbmVkRmllbGRDb3VudCxcbiAgICAgICAgICAgICAgICBzY2hlbWE6IHRoaXMuYmJpLnNjaGVtYX07XG4gICAgaWYgKHRoaXMuYmJpLnR5cGUgPT09ICdiaWdiZWQnKSB7XG4gICAgICAgIHRoaXMuYmJpLmdldEV4dHJhSW5kaWNlcyhmdW5jdGlvbihlaSkge1xuICAgICAgICAgICAgaWYgKGVpKSB7XG4gICAgICAgICAgICAgICAgdGhpc0IuZXh0cmFJbmRpY2VzID0gZWk7XG4gICAgICAgICAgICAgICAgbWV0YS5leHRyYUluZGljZXMgPSBlaS5tYXAoZnVuY3Rpb24oaSkge3JldHVybiBpLmZpZWxkfSk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogbWV0YX0pO1xuICAgICAgICB9KTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogbWV0YX0pO1xuICAgIH1cbn1cblxuQkJJV29ya2VyRmV0Y2hlci5wcm90b3R5cGUubGVhcCA9IGZ1bmN0aW9uKHRhZywgY2hyLCBwb3MsIGRpcikge1xuICAgIHRoaXMuYmJpLmdldFVuem9vbWVkVmlldygpLmdldEZpcnN0QWRqYWNlbnQoY2hyLCBwb3MsIGRpciwgZnVuY3Rpb24ocmVzdWx0LCBlcnIpIHtcbiAgICAgICAgcG9zdE1lc3NhZ2Uoe3RhZzogdGFnLCByZXN1bHQ6IHJlc3VsdCwgZXJyb3I6IGVycn0pO1xuICAgIH0pO1xufVxuXG5CQklXb3JrZXJGZXRjaGVyLnByb3RvdHlwZS5xdWFudExlYXAgPSBmdW5jdGlvbih0YWcsIGNociwgcG9zLCBkaXIsIHRocmVzaG9sZCwgdW5kZXIpIHtcbiAgICB0aGlzLmJiaS50aHJlc2hvbGRTZWFyY2goY2hyLCBwb3MsIGRpciwgdGhyZXNob2xkLCBmdW5jdGlvbihyZXN1bHQsIGVycikge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogcmVzdWx0LCBlcnJvcjogZXJyfSk7XG4gICAgfSk7XG59XG5cbkJCSVdvcmtlckZldGNoZXIucHJvdG90eXBlLnNlYXJjaCA9IGZ1bmN0aW9uKHRhZywgcXVlcnksIGluZGV4KSB7XG4gICAgdmFyIGlzID0gdGhpcy5leHRyYUluZGljZXNbMF07XG4gICAgaXMubG9va3VwKHF1ZXJ5LCBmdW5jdGlvbihyZXN1bHQsIGVycikge1xuICAgICAgICBwb3N0TWVzc2FnZSh7dGFnOiB0YWcsIHJlc3VsdDogcmVzdWx0LCBlcnJvcjogZXJyfSk7XG4gICAgfSk7XG59XG5cbn0pLmNhbGwodGhpcyx0eXBlb2Ygc2VsZiAhPT0gXCJ1bmRlZmluZWRcIiA/IHNlbGYgOiB0eXBlb2Ygd2luZG93ICE9PSBcInVuZGVmaW5lZFwiID8gd2luZG93IDoge30pIiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gRGFsbGlhbmNlIEdlbm9tZSBFeHBsb3JlclxuLy8gKGMpIFRob21hcyBEb3duIDIwMDYtMjAxMVxuLy9cbi8vIGxoM3V0aWxzLmpzOiBjb21tb24gc3VwcG9ydCBmb3IgbGgzJ3MgZmlsZSBmb3JtYXRzXG4vL1xuXG5pZiAodHlwZW9mKHJlcXVpcmUpICE9PSAndW5kZWZpbmVkJykge1xuICAgIHZhciBqc3psaWIgPSByZXF1aXJlKCdqc3psaWInKTtcbiAgICB2YXIganN6bGliX2luZmxhdGVfYnVmZmVyID0ganN6bGliLmluZmxhdGVCdWZmZXI7XG4gICAgdmFyIGFycmF5Q29weSA9IGpzemxpYi5hcnJheUNvcHk7XG59XG5cbmZ1bmN0aW9uIFZvYihiLCBvKSB7XG4gICAgdGhpcy5ibG9jayA9IGI7XG4gICAgdGhpcy5vZmZzZXQgPSBvO1xufVxuXG5Wb2IucHJvdG90eXBlLnRvU3RyaW5nID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuICcnICsgdGhpcy5ibG9jayArICc6JyArIHRoaXMub2Zmc2V0O1xufVxuXG5mdW5jdGlvbiByZWFkVm9iKGJhLCBvZmZzZXQpIHtcbiAgICB2YXIgYmxvY2sgPSAoKGJhW29mZnNldCs2XSAmIDB4ZmYpICogMHgxMDAwMDAwMDApICsgKChiYVtvZmZzZXQrNV0gJiAweGZmKSAqIDB4MTAwMDAwMCkgKyAoKGJhW29mZnNldCs0XSAmIDB4ZmYpICogMHgxMDAwMCkgKyAoKGJhW29mZnNldCszXSAmIDB4ZmYpICogMHgxMDApICsgKChiYVtvZmZzZXQrMl0gJiAweGZmKSk7XG4gICAgdmFyIGJpbnQgPSAoYmFbb2Zmc2V0KzFdIDw8IDgpIHwgKGJhW29mZnNldF0pO1xuICAgIGlmIChibG9jayA9PSAwICYmIGJpbnQgPT0gMCkge1xuICAgICAgICByZXR1cm4gbnVsbDsgIC8vIFNob3VsZCBvbmx5IGhhcHBlbiBpbiB0aGUgbGluZWFyIGluZGV4P1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiBuZXcgVm9iKGJsb2NrLCBiaW50KTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIHVuYmd6ZihkYXRhLCBsaW0pIHtcbiAgICBsaW0gPSBNYXRoLm1pbihsaW0gfHwgMSwgZGF0YS5ieXRlTGVuZ3RoIC0gNTApO1xuICAgIHZhciBvQmxvY2tMaXN0ID0gW107XG4gICAgdmFyIHB0ciA9IFswXTtcbiAgICB2YXIgdG90YWxTaXplID0gMDtcblxuICAgIHdoaWxlIChwdHJbMF0gPCBsaW0pIHtcbiAgICAgICAgdmFyIGJhID0gbmV3IFVpbnQ4QXJyYXkoZGF0YSwgcHRyWzBdLCAxMik7IC8vIEZJWE1FIGlzIHRoaXMgZW5vdWdoIGZvciBhbGwgY3JlZGlibGUgQkdaRiBibG9jayBoZWFkZXJzP1xuICAgICAgICB2YXIgeGxlbiA9IChiYVsxMV0gPDwgOCkgfCAoYmFbMTBdKTtcbiAgICAgICAgLy8gZGxvZygneGxlblsnICsgKHB0clswXSkgKyddPScgKyB4bGVuKTtcbiAgICAgICAgdmFyIHVuYyA9IGpzemxpYl9pbmZsYXRlX2J1ZmZlcihkYXRhLCAxMiArIHhsZW4gKyBwdHJbMF0sIE1hdGgubWluKDY1NTM2LCBkYXRhLmJ5dGVMZW5ndGggLSAxMiAtIHhsZW4gLSBwdHJbMF0pLCBwdHIpO1xuICAgICAgICBwdHJbMF0gKz0gODtcbiAgICAgICAgdG90YWxTaXplICs9IHVuYy5ieXRlTGVuZ3RoO1xuICAgICAgICBvQmxvY2tMaXN0LnB1c2godW5jKTtcbiAgICB9XG5cbiAgICBpZiAob0Jsb2NrTGlzdC5sZW5ndGggPT0gMSkge1xuICAgICAgICByZXR1cm4gb0Jsb2NrTGlzdFswXTtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgb3V0ID0gbmV3IFVpbnQ4QXJyYXkodG90YWxTaXplKTtcbiAgICAgICAgdmFyIGN1cnNvciA9IDA7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb0Jsb2NrTGlzdC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGIgPSBuZXcgVWludDhBcnJheShvQmxvY2tMaXN0W2ldKTtcbiAgICAgICAgICAgIGFycmF5Q29weShiLCAwLCBvdXQsIGN1cnNvciwgYi5sZW5ndGgpO1xuICAgICAgICAgICAgY3Vyc29yICs9IGIubGVuZ3RoO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiBvdXQuYnVmZmVyO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gQ2h1bmsobWludiwgbWF4dikge1xuICAgIHRoaXMubWludiA9IG1pbnY7IHRoaXMubWF4diA9IG1heHY7XG59XG5cblxuLy9cbi8vIEJpbm5pbmcgKHRyYW5zbGl0ZXJhdGVkIGZyb20gU0FNMS4zIHNwZWMpXG4vL1xuXG4vKiBjYWxjdWxhdGUgYmluIGdpdmVuIGFuIGFsaWdubWVudCBjb3ZlcmluZyBbYmVnLGVuZCkgKHplcm8tYmFzZWQsIGhhbGYtY2xvc2UtaGFsZi1vcGVuKSAqL1xuZnVuY3Rpb24gcmVnMmJpbihiZWcsIGVuZClcbntcbiAgICAtLWVuZDtcbiAgICBpZiAoYmVnPj4xNCA9PSBlbmQ+PjE0KSByZXR1cm4gKCgxPDwxNSktMSkvNyArIChiZWc+PjE0KTtcbiAgICBpZiAoYmVnPj4xNyA9PSBlbmQ+PjE3KSByZXR1cm4gKCgxPDwxMiktMSkvNyArIChiZWc+PjE3KTtcbiAgICBpZiAoYmVnPj4yMCA9PSBlbmQ+PjIwKSByZXR1cm4gKCgxPDw5KS0xKS83ICsgKGJlZz4+MjApO1xuICAgIGlmIChiZWc+PjIzID09IGVuZD4+MjMpIHJldHVybiAoKDE8PDYpLTEpLzcgKyAoYmVnPj4yMyk7XG4gICAgaWYgKGJlZz4+MjYgPT0gZW5kPj4yNikgcmV0dXJuICgoMTw8MyktMSkvNyArIChiZWc+PjI2KTtcbiAgICByZXR1cm4gMDtcbn1cblxuLyogY2FsY3VsYXRlIHRoZSBsaXN0IG9mIGJpbnMgdGhhdCBtYXkgb3ZlcmxhcCB3aXRoIHJlZ2lvbiBbYmVnLGVuZCkgKHplcm8tYmFzZWQpICovXG52YXIgTUFYX0JJTiA9ICgoKDE8PDE4KS0xKS83KTtcbmZ1bmN0aW9uIHJlZzJiaW5zKGJlZywgZW5kKSBcbntcbiAgICB2YXIgaSA9IDAsIGssIGxpc3QgPSBbXTtcbiAgICAtLWVuZDtcbiAgICBsaXN0LnB1c2goMCk7XG4gICAgZm9yIChrID0gMSArIChiZWc+PjI2KTsgayA8PSAxICsgKGVuZD4+MjYpOyArK2spIGxpc3QucHVzaChrKTtcbiAgICBmb3IgKGsgPSA5ICsgKGJlZz4+MjMpOyBrIDw9IDkgKyAoZW5kPj4yMyk7ICsraykgbGlzdC5wdXNoKGspO1xuICAgIGZvciAoayA9IDczICsgKGJlZz4+MjApOyBrIDw9IDczICsgKGVuZD4+MjApOyArK2spIGxpc3QucHVzaChrKTtcbiAgICBmb3IgKGsgPSA1ODUgKyAoYmVnPj4xNyk7IGsgPD0gNTg1ICsgKGVuZD4+MTcpOyArK2spIGxpc3QucHVzaChrKTtcbiAgICBmb3IgKGsgPSA0NjgxICsgKGJlZz4+MTQpOyBrIDw9IDQ2ODEgKyAoZW5kPj4xNCk7ICsraykgbGlzdC5wdXNoKGspO1xuICAgIHJldHVybiBsaXN0O1xufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIHVuYmd6ZjogdW5iZ3pmLFxuICAgICAgICByZWFkVm9iOiByZWFkVm9iLFxuICAgICAgICByZWcyYmluOiByZWcyYmluLFxuICAgICAgICByZWcyYmluczogcmVnMmJpbnMsXG4gICAgICAgIENodW5rOiBDaHVua1xuICAgIH07XG59IiwiLypcclxuICogQSBKYXZhU2NyaXB0IGltcGxlbWVudGF0aW9uIG9mIHRoZSBTZWN1cmUgSGFzaCBBbGdvcml0aG0sIFNIQS0xLCBhcyBkZWZpbmVkXHJcbiAqIGluIEZJUFMgMTgwLTFcclxuICogVmVyc2lvbiAyLjIgQ29weXJpZ2h0IFBhdWwgSm9obnN0b24gMjAwMCAtIDIwMDkuXHJcbiAqIE90aGVyIGNvbnRyaWJ1dG9yczogR3JlZyBIb2x0LCBBbmRyZXcgS2VwZXJ0LCBZZG5hciwgTG9zdGluZXRcclxuICogRGlzdHJpYnV0ZWQgdW5kZXIgdGhlIEJTRCBMaWNlbnNlXHJcbiAqIFNlZSBodHRwOi8vcGFqaG9tZS5vcmcudWsvY3J5cHQvbWQ1IGZvciBkZXRhaWxzLlxyXG4gKi9cclxuXHJcbiBcInVzZSBzdHJpY3RcIjtcclxuXHJcbi8qXHJcbiAqIENvbmZpZ3VyYWJsZSB2YXJpYWJsZXMuIFlvdSBtYXkgbmVlZCB0byB0d2VhayB0aGVzZSB0byBiZSBjb21wYXRpYmxlIHdpdGhcclxuICogdGhlIHNlcnZlci1zaWRlLCBidXQgdGhlIGRlZmF1bHRzIHdvcmsgaW4gbW9zdCBjYXNlcy5cclxuICovXHJcbnZhciBoZXhjYXNlID0gMDsgIC8qIGhleCBvdXRwdXQgZm9ybWF0LiAwIC0gbG93ZXJjYXNlOyAxIC0gdXBwZXJjYXNlICAgICAgICAqL1xyXG52YXIgYjY0cGFkICA9IFwiXCI7IC8qIGJhc2UtNjQgcGFkIGNoYXJhY3Rlci4gXCI9XCIgZm9yIHN0cmljdCBSRkMgY29tcGxpYW5jZSAgICovXHJcblxyXG4vKlxyXG4gKiBUaGVzZSBhcmUgdGhlIGZ1bmN0aW9ucyB5b3UnbGwgdXN1YWxseSB3YW50IHRvIGNhbGxcclxuICogVGhleSB0YWtlIHN0cmluZyBhcmd1bWVudHMgYW5kIHJldHVybiBlaXRoZXIgaGV4IG9yIGJhc2UtNjQgZW5jb2RlZCBzdHJpbmdzXHJcbiAqL1xyXG5mdW5jdGlvbiBoZXhfc2hhMShzKSAgICB7IHJldHVybiByc3RyMmhleChyc3RyX3NoYTEoc3RyMnJzdHJfdXRmOChzKSkpOyB9XHJcbmZ1bmN0aW9uIGI2NF9zaGExKHMpICAgIHsgcmV0dXJuIHJzdHIyYjY0KHJzdHJfc2hhMShzdHIycnN0cl91dGY4KHMpKSk7IH1cclxuZnVuY3Rpb24gYW55X3NoYTEocywgZSkgeyByZXR1cm4gcnN0cjJhbnkocnN0cl9zaGExKHN0cjJyc3RyX3V0ZjgocykpLCBlKTsgfVxyXG5mdW5jdGlvbiBoZXhfaG1hY19zaGExKGssIGQpXHJcbiAgeyByZXR1cm4gcnN0cjJoZXgocnN0cl9obWFjX3NoYTEoc3RyMnJzdHJfdXRmOChrKSwgc3RyMnJzdHJfdXRmOChkKSkpOyB9XHJcbmZ1bmN0aW9uIGI2NF9obWFjX3NoYTEoaywgZClcclxuICB7IHJldHVybiByc3RyMmI2NChyc3RyX2htYWNfc2hhMShzdHIycnN0cl91dGY4KGspLCBzdHIycnN0cl91dGY4KGQpKSk7IH1cclxuZnVuY3Rpb24gYW55X2htYWNfc2hhMShrLCBkLCBlKVxyXG4gIHsgcmV0dXJuIHJzdHIyYW55KHJzdHJfaG1hY19zaGExKHN0cjJyc3RyX3V0ZjgoayksIHN0cjJyc3RyX3V0ZjgoZCkpLCBlKTsgfVxyXG5cclxuLypcclxuICogUGVyZm9ybSBhIHNpbXBsZSBzZWxmLXRlc3QgdG8gc2VlIGlmIHRoZSBWTSBpcyB3b3JraW5nXHJcbiAqL1xyXG5mdW5jdGlvbiBzaGExX3ZtX3Rlc3QoKVxyXG57XHJcbiAgcmV0dXJuIGhleF9zaGExKFwiYWJjXCIpLnRvTG93ZXJDYXNlKCkgPT0gXCJhOTk5M2UzNjQ3MDY4MTZhYmEzZTI1NzE3ODUwYzI2YzljZDBkODlkXCI7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENhbGN1bGF0ZSB0aGUgU0hBMSBvZiBhIHJhdyBzdHJpbmdcclxuICovXHJcbmZ1bmN0aW9uIHJzdHJfc2hhMShzKVxyXG57XHJcbiAgcmV0dXJuIGJpbmIycnN0cihiaW5iX3NoYTEocnN0cjJiaW5iKHMpLCBzLmxlbmd0aCAqIDgpKTtcclxufVxyXG5cclxuLypcclxuICogQ2FsY3VsYXRlIHRoZSBITUFDLVNIQTEgb2YgYSBrZXkgYW5kIHNvbWUgZGF0YSAocmF3IHN0cmluZ3MpXHJcbiAqL1xyXG5mdW5jdGlvbiByc3RyX2htYWNfc2hhMShrZXksIGRhdGEpXHJcbntcclxuICB2YXIgYmtleSA9IHJzdHIyYmluYihrZXkpO1xyXG4gIGlmKGJrZXkubGVuZ3RoID4gMTYpIGJrZXkgPSBiaW5iX3NoYTEoYmtleSwga2V5Lmxlbmd0aCAqIDgpO1xyXG5cclxuICB2YXIgaXBhZCA9IEFycmF5KDE2KSwgb3BhZCA9IEFycmF5KDE2KTtcclxuICBmb3IodmFyIGkgPSAwOyBpIDwgMTY7IGkrKylcclxuICB7XHJcbiAgICBpcGFkW2ldID0gYmtleVtpXSBeIDB4MzYzNjM2MzY7XHJcbiAgICBvcGFkW2ldID0gYmtleVtpXSBeIDB4NUM1QzVDNUM7XHJcbiAgfVxyXG5cclxuICB2YXIgaGFzaCA9IGJpbmJfc2hhMShpcGFkLmNvbmNhdChyc3RyMmJpbmIoZGF0YSkpLCA1MTIgKyBkYXRhLmxlbmd0aCAqIDgpO1xyXG4gIHJldHVybiBiaW5iMnJzdHIoYmluYl9zaGExKG9wYWQuY29uY2F0KGhhc2gpLCA1MTIgKyAxNjApKTtcclxufVxyXG5cclxuLypcclxuICogQ29udmVydCBhIHJhdyBzdHJpbmcgdG8gYSBoZXggc3RyaW5nXHJcbiAqL1xyXG5mdW5jdGlvbiByc3RyMmhleChpbnB1dClcclxue1xyXG4gIC8vIHRyeSB7IGhleGNhc2UgfSBjYXRjaChlKSB7IGhleGNhc2U9MDsgfVxyXG4gIHZhciBoZXhfdGFiID0gaGV4Y2FzZSA/IFwiMDEyMzQ1Njc4OUFCQ0RFRlwiIDogXCIwMTIzNDU2Nzg5YWJjZGVmXCI7XHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgdmFyIHg7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IGlucHV0Lmxlbmd0aDsgaSsrKVxyXG4gIHtcclxuICAgIHggPSBpbnB1dC5jaGFyQ29kZUF0KGkpO1xyXG4gICAgb3V0cHV0ICs9IGhleF90YWIuY2hhckF0KCh4ID4+PiA0KSAmIDB4MEYpXHJcbiAgICAgICAgICAgKyAgaGV4X3RhYi5jaGFyQXQoIHggICAgICAgICYgMHgwRik7XHJcbiAgfVxyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENvbnZlcnQgYSByYXcgc3RyaW5nIHRvIGEgYmFzZS02NCBzdHJpbmdcclxuICovXHJcbmZ1bmN0aW9uIHJzdHIyYjY0KGlucHV0KVxyXG57XHJcbiAgLy8gdHJ5IHsgYjY0cGFkIH0gY2F0Y2goZSkgeyBiNjRwYWQ9Jyc7IH1cclxuICB2YXIgdGFiID0gXCJBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWmFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6MDEyMzQ1Njc4OSsvXCI7XHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgdmFyIGxlbiA9IGlucHV0Lmxlbmd0aDtcclxuICBmb3IodmFyIGkgPSAwOyBpIDwgbGVuOyBpICs9IDMpXHJcbiAge1xyXG4gICAgdmFyIHRyaXBsZXQgPSAoaW5wdXQuY2hhckNvZGVBdChpKSA8PCAxNilcclxuICAgICAgICAgICAgICAgIHwgKGkgKyAxIDwgbGVuID8gaW5wdXQuY2hhckNvZGVBdChpKzEpIDw8IDggOiAwKVxyXG4gICAgICAgICAgICAgICAgfCAoaSArIDIgPCBsZW4gPyBpbnB1dC5jaGFyQ29kZUF0KGkrMikgICAgICA6IDApO1xyXG4gICAgZm9yKHZhciBqID0gMDsgaiA8IDQ7IGorKylcclxuICAgIHtcclxuICAgICAgaWYoaSAqIDggKyBqICogNiA+IGlucHV0Lmxlbmd0aCAqIDgpIG91dHB1dCArPSBiNjRwYWQ7XHJcbiAgICAgIGVsc2Ugb3V0cHV0ICs9IHRhYi5jaGFyQXQoKHRyaXBsZXQgPj4+IDYqKDMtaikpICYgMHgzRik7XHJcbiAgICB9XHJcbiAgfVxyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENvbnZlcnQgYSByYXcgc3RyaW5nIHRvIGFuIGFyYml0cmFyeSBzdHJpbmcgZW5jb2RpbmdcclxuICovXHJcbmZ1bmN0aW9uIHJzdHIyYW55KGlucHV0LCBlbmNvZGluZylcclxue1xyXG4gIHZhciBkaXZpc29yID0gZW5jb2RpbmcubGVuZ3RoO1xyXG4gIHZhciByZW1haW5kZXJzID0gQXJyYXkoKTtcclxuICB2YXIgaSwgcSwgeCwgcXVvdGllbnQ7XHJcblxyXG4gIC8qIENvbnZlcnQgdG8gYW4gYXJyYXkgb2YgMTYtYml0IGJpZy1lbmRpYW4gdmFsdWVzLCBmb3JtaW5nIHRoZSBkaXZpZGVuZCAqL1xyXG4gIHZhciBkaXZpZGVuZCA9IEFycmF5KE1hdGguY2VpbChpbnB1dC5sZW5ndGggLyAyKSk7XHJcbiAgZm9yKGkgPSAwOyBpIDwgZGl2aWRlbmQubGVuZ3RoOyBpKyspXHJcbiAge1xyXG4gICAgZGl2aWRlbmRbaV0gPSAoaW5wdXQuY2hhckNvZGVBdChpICogMikgPDwgOCkgfCBpbnB1dC5jaGFyQ29kZUF0KGkgKiAyICsgMSk7XHJcbiAgfVxyXG5cclxuICAvKlxyXG4gICAqIFJlcGVhdGVkbHkgcGVyZm9ybSBhIGxvbmcgZGl2aXNpb24uIFRoZSBiaW5hcnkgYXJyYXkgZm9ybXMgdGhlIGRpdmlkZW5kLFxyXG4gICAqIHRoZSBsZW5ndGggb2YgdGhlIGVuY29kaW5nIGlzIHRoZSBkaXZpc29yLiBPbmNlIGNvbXB1dGVkLCB0aGUgcXVvdGllbnRcclxuICAgKiBmb3JtcyB0aGUgZGl2aWRlbmQgZm9yIHRoZSBuZXh0IHN0ZXAuIFdlIHN0b3Agd2hlbiB0aGUgZGl2aWRlbmQgaXMgemVyby5cclxuICAgKiBBbGwgcmVtYWluZGVycyBhcmUgc3RvcmVkIGZvciBsYXRlciB1c2UuXHJcbiAgICovXHJcbiAgd2hpbGUoZGl2aWRlbmQubGVuZ3RoID4gMClcclxuICB7XHJcbiAgICBxdW90aWVudCA9IEFycmF5KCk7XHJcbiAgICB4ID0gMDtcclxuICAgIGZvcihpID0gMDsgaSA8IGRpdmlkZW5kLmxlbmd0aDsgaSsrKVxyXG4gICAge1xyXG4gICAgICB4ID0gKHggPDwgMTYpICsgZGl2aWRlbmRbaV07XHJcbiAgICAgIHEgPSBNYXRoLmZsb29yKHggLyBkaXZpc29yKTtcclxuICAgICAgeCAtPSBxICogZGl2aXNvcjtcclxuICAgICAgaWYocXVvdGllbnQubGVuZ3RoID4gMCB8fCBxID4gMClcclxuICAgICAgICBxdW90aWVudFtxdW90aWVudC5sZW5ndGhdID0gcTtcclxuICAgIH1cclxuICAgIHJlbWFpbmRlcnNbcmVtYWluZGVycy5sZW5ndGhdID0geDtcclxuICAgIGRpdmlkZW5kID0gcXVvdGllbnQ7XHJcbiAgfVxyXG5cclxuICAvKiBDb252ZXJ0IHRoZSByZW1haW5kZXJzIHRvIHRoZSBvdXRwdXQgc3RyaW5nICovXHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgZm9yKGkgPSByZW1haW5kZXJzLmxlbmd0aCAtIDE7IGkgPj0gMDsgaS0tKVxyXG4gICAgb3V0cHV0ICs9IGVuY29kaW5nLmNoYXJBdChyZW1haW5kZXJzW2ldKTtcclxuXHJcbiAgLyogQXBwZW5kIGxlYWRpbmcgemVybyBlcXVpdmFsZW50cyAqL1xyXG4gIHZhciBmdWxsX2xlbmd0aCA9IE1hdGguY2VpbChpbnB1dC5sZW5ndGggKiA4IC9cclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgKE1hdGgubG9nKGVuY29kaW5nLmxlbmd0aCkgLyBNYXRoLmxvZygyKSkpXHJcbiAgZm9yKGkgPSBvdXRwdXQubGVuZ3RoOyBpIDwgZnVsbF9sZW5ndGg7IGkrKylcclxuICAgIG91dHB1dCA9IGVuY29kaW5nWzBdICsgb3V0cHV0O1xyXG5cclxuICByZXR1cm4gb3V0cHV0O1xyXG59XHJcblxyXG4vKlxyXG4gKiBFbmNvZGUgYSBzdHJpbmcgYXMgdXRmLTguXHJcbiAqIEZvciBlZmZpY2llbmN5LCB0aGlzIGFzc3VtZXMgdGhlIGlucHV0IGlzIHZhbGlkIHV0Zi0xNi5cclxuICovXHJcbmZ1bmN0aW9uIHN0cjJyc3RyX3V0ZjgoaW5wdXQpXHJcbntcclxuICB2YXIgb3V0cHV0ID0gXCJcIjtcclxuICB2YXIgaSA9IC0xO1xyXG4gIHZhciB4LCB5O1xyXG5cclxuICB3aGlsZSgrK2kgPCBpbnB1dC5sZW5ndGgpXHJcbiAge1xyXG4gICAgLyogRGVjb2RlIHV0Zi0xNiBzdXJyb2dhdGUgcGFpcnMgKi9cclxuICAgIHggPSBpbnB1dC5jaGFyQ29kZUF0KGkpO1xyXG4gICAgeSA9IGkgKyAxIDwgaW5wdXQubGVuZ3RoID8gaW5wdXQuY2hhckNvZGVBdChpICsgMSkgOiAwO1xyXG4gICAgaWYoMHhEODAwIDw9IHggJiYgeCA8PSAweERCRkYgJiYgMHhEQzAwIDw9IHkgJiYgeSA8PSAweERGRkYpXHJcbiAgICB7XHJcbiAgICAgIHggPSAweDEwMDAwICsgKCh4ICYgMHgwM0ZGKSA8PCAxMCkgKyAoeSAmIDB4MDNGRik7XHJcbiAgICAgIGkrKztcclxuICAgIH1cclxuXHJcbiAgICAvKiBFbmNvZGUgb3V0cHV0IGFzIHV0Zi04ICovXHJcbiAgICBpZih4IDw9IDB4N0YpXHJcbiAgICAgIG91dHB1dCArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKHgpO1xyXG4gICAgZWxzZSBpZih4IDw9IDB4N0ZGKVxyXG4gICAgICBvdXRwdXQgKz0gU3RyaW5nLmZyb21DaGFyQ29kZSgweEMwIHwgKCh4ID4+PiA2ICkgJiAweDFGKSxcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMHg4MCB8ICggeCAgICAgICAgICYgMHgzRikpO1xyXG4gICAgZWxzZSBpZih4IDw9IDB4RkZGRilcclxuICAgICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoMHhFMCB8ICgoeCA+Pj4gMTIpICYgMHgwRiksXHJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDB4ODAgfCAoKHggPj4+IDYgKSAmIDB4M0YpLFxyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAweDgwIHwgKCB4ICAgICAgICAgJiAweDNGKSk7XHJcbiAgICBlbHNlIGlmKHggPD0gMHgxRkZGRkYpXHJcbiAgICAgIG91dHB1dCArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKDB4RjAgfCAoKHggPj4+IDE4KSAmIDB4MDcpLFxyXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAweDgwIHwgKCh4ID4+PiAxMikgJiAweDNGKSxcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgMHg4MCB8ICgoeCA+Pj4gNiApICYgMHgzRiksXHJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIDB4ODAgfCAoIHggICAgICAgICAmIDB4M0YpKTtcclxuICB9XHJcbiAgcmV0dXJuIG91dHB1dDtcclxufVxyXG5cclxuLypcclxuICogRW5jb2RlIGEgc3RyaW5nIGFzIHV0Zi0xNlxyXG4gKi9cclxuZnVuY3Rpb24gc3RyMnJzdHJfdXRmMTZsZShpbnB1dClcclxue1xyXG4gIHZhciBvdXRwdXQgPSBcIlwiO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCBpbnB1dC5sZW5ndGg7IGkrKylcclxuICAgIG91dHB1dCArPSBTdHJpbmcuZnJvbUNoYXJDb2RlKCBpbnB1dC5jaGFyQ29kZUF0KGkpICAgICAgICAmIDB4RkYsXHJcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAoaW5wdXQuY2hhckNvZGVBdChpKSA+Pj4gOCkgJiAweEZGKTtcclxuICByZXR1cm4gb3V0cHV0O1xyXG59XHJcblxyXG5mdW5jdGlvbiBzdHIycnN0cl91dGYxNmJlKGlucHV0KVxyXG57XHJcbiAgdmFyIG91dHB1dCA9IFwiXCI7XHJcbiAgZm9yKHZhciBpID0gMDsgaSA8IGlucHV0Lmxlbmd0aDsgaSsrKVxyXG4gICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoKGlucHV0LmNoYXJDb2RlQXQoaSkgPj4+IDgpICYgMHhGRixcclxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbnB1dC5jaGFyQ29kZUF0KGkpICAgICAgICAmIDB4RkYpO1xyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENvbnZlcnQgYSByYXcgc3RyaW5nIHRvIGFuIGFycmF5IG9mIGJpZy1lbmRpYW4gd29yZHNcclxuICogQ2hhcmFjdGVycyA+MjU1IGhhdmUgdGhlaXIgaGlnaC1ieXRlIHNpbGVudGx5IGlnbm9yZWQuXHJcbiAqL1xyXG5mdW5jdGlvbiByc3RyMmJpbmIoaW5wdXQpXHJcbntcclxuICB2YXIgb3V0cHV0ID0gQXJyYXkoaW5wdXQubGVuZ3RoID4+IDIpO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCBvdXRwdXQubGVuZ3RoOyBpKyspXHJcbiAgICBvdXRwdXRbaV0gPSAwO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCBpbnB1dC5sZW5ndGggKiA4OyBpICs9IDgpXHJcbiAgICBvdXRwdXRbaT4+NV0gfD0gKGlucHV0LmNoYXJDb2RlQXQoaSAvIDgpICYgMHhGRikgPDwgKDI0IC0gaSAlIDMyKTtcclxuICByZXR1cm4gb3V0cHV0O1xyXG59XHJcblxyXG4vKlxyXG4gKiBDb252ZXJ0IGFuIGFycmF5IG9mIGJpZy1lbmRpYW4gd29yZHMgdG8gYSBzdHJpbmdcclxuICovXHJcbmZ1bmN0aW9uIGJpbmIycnN0cihpbnB1dClcclxue1xyXG4gIHZhciBvdXRwdXQgPSBcIlwiO1xyXG4gIGZvcih2YXIgaSA9IDA7IGkgPCBpbnB1dC5sZW5ndGggKiAzMjsgaSArPSA4KVxyXG4gICAgb3V0cHV0ICs9IFN0cmluZy5mcm9tQ2hhckNvZGUoKGlucHV0W2k+PjVdID4+PiAoMjQgLSBpICUgMzIpKSAmIDB4RkYpO1xyXG4gIHJldHVybiBvdXRwdXQ7XHJcbn1cclxuXHJcbi8qXHJcbiAqIENhbGN1bGF0ZSB0aGUgU0hBLTEgb2YgYW4gYXJyYXkgb2YgYmlnLWVuZGlhbiB3b3JkcywgYW5kIGEgYml0IGxlbmd0aFxyXG4gKi9cclxuZnVuY3Rpb24gYmluYl9zaGExKHgsIGxlbilcclxue1xyXG4gIC8qIGFwcGVuZCBwYWRkaW5nICovXHJcbiAgeFtsZW4gPj4gNV0gfD0gMHg4MCA8PCAoMjQgLSBsZW4gJSAzMik7XHJcbiAgeFsoKGxlbiArIDY0ID4+IDkpIDw8IDQpICsgMTVdID0gbGVuO1xyXG5cclxuICB2YXIgdyA9IEFycmF5KDgwKTtcclxuICB2YXIgYSA9ICAxNzMyNTg0MTkzO1xyXG4gIHZhciBiID0gLTI3MTczMzg3OTtcclxuICB2YXIgYyA9IC0xNzMyNTg0MTk0O1xyXG4gIHZhciBkID0gIDI3MTczMzg3ODtcclxuICB2YXIgZSA9IC0xMDA5NTg5Nzc2O1xyXG5cclxuICBmb3IodmFyIGkgPSAwOyBpIDwgeC5sZW5ndGg7IGkgKz0gMTYpXHJcbiAge1xyXG4gICAgdmFyIG9sZGEgPSBhO1xyXG4gICAgdmFyIG9sZGIgPSBiO1xyXG4gICAgdmFyIG9sZGMgPSBjO1xyXG4gICAgdmFyIG9sZGQgPSBkO1xyXG4gICAgdmFyIG9sZGUgPSBlO1xyXG5cclxuICAgIGZvcih2YXIgaiA9IDA7IGogPCA4MDsgaisrKVxyXG4gICAge1xyXG4gICAgICBpZihqIDwgMTYpIHdbal0gPSB4W2kgKyBqXTtcclxuICAgICAgZWxzZSB3W2pdID0gYml0X3JvbCh3W2otM10gXiB3W2otOF0gXiB3W2otMTRdIF4gd1tqLTE2XSwgMSk7XHJcbiAgICAgIHZhciB0ID0gc2FmZV9hZGQoc2FmZV9hZGQoYml0X3JvbChhLCA1KSwgc2hhMV9mdChqLCBiLCBjLCBkKSksXHJcbiAgICAgICAgICAgICAgICAgICAgICAgc2FmZV9hZGQoc2FmZV9hZGQoZSwgd1tqXSksIHNoYTFfa3QoaikpKTtcclxuICAgICAgZSA9IGQ7XHJcbiAgICAgIGQgPSBjO1xyXG4gICAgICBjID0gYml0X3JvbChiLCAzMCk7XHJcbiAgICAgIGIgPSBhO1xyXG4gICAgICBhID0gdDtcclxuICAgIH1cclxuXHJcbiAgICBhID0gc2FmZV9hZGQoYSwgb2xkYSk7XHJcbiAgICBiID0gc2FmZV9hZGQoYiwgb2xkYik7XHJcbiAgICBjID0gc2FmZV9hZGQoYywgb2xkYyk7XHJcbiAgICBkID0gc2FmZV9hZGQoZCwgb2xkZCk7XHJcbiAgICBlID0gc2FmZV9hZGQoZSwgb2xkZSk7XHJcbiAgfVxyXG4gIHJldHVybiBBcnJheShhLCBiLCBjLCBkLCBlKTtcclxuXHJcbn1cclxuXHJcbi8qXHJcbiAqIFBlcmZvcm0gdGhlIGFwcHJvcHJpYXRlIHRyaXBsZXQgY29tYmluYXRpb24gZnVuY3Rpb24gZm9yIHRoZSBjdXJyZW50XHJcbiAqIGl0ZXJhdGlvblxyXG4gKi9cclxuZnVuY3Rpb24gc2hhMV9mdCh0LCBiLCBjLCBkKVxyXG57XHJcbiAgaWYodCA8IDIwKSByZXR1cm4gKGIgJiBjKSB8ICgofmIpICYgZCk7XHJcbiAgaWYodCA8IDQwKSByZXR1cm4gYiBeIGMgXiBkO1xyXG4gIGlmKHQgPCA2MCkgcmV0dXJuIChiICYgYykgfCAoYiAmIGQpIHwgKGMgJiBkKTtcclxuICByZXR1cm4gYiBeIGMgXiBkO1xyXG59XHJcblxyXG4vKlxyXG4gKiBEZXRlcm1pbmUgdGhlIGFwcHJvcHJpYXRlIGFkZGl0aXZlIGNvbnN0YW50IGZvciB0aGUgY3VycmVudCBpdGVyYXRpb25cclxuICovXHJcbmZ1bmN0aW9uIHNoYTFfa3QodClcclxue1xyXG4gIHJldHVybiAodCA8IDIwKSA/ICAxNTE4NTAwMjQ5IDogKHQgPCA0MCkgPyAgMTg1OTc3NTM5MyA6XHJcbiAgICAgICAgICh0IDwgNjApID8gLTE4OTQwMDc1ODggOiAtODk5NDk3NTE0O1xyXG59XHJcblxyXG4vKlxyXG4gKiBBZGQgaW50ZWdlcnMsIHdyYXBwaW5nIGF0IDJeMzIuIFRoaXMgdXNlcyAxNi1iaXQgb3BlcmF0aW9ucyBpbnRlcm5hbGx5XHJcbiAqIHRvIHdvcmsgYXJvdW5kIGJ1Z3MgaW4gc29tZSBKUyBpbnRlcnByZXRlcnMuXHJcbiAqL1xyXG5mdW5jdGlvbiBzYWZlX2FkZCh4LCB5KVxyXG57XHJcbiAgdmFyIGxzdyA9ICh4ICYgMHhGRkZGKSArICh5ICYgMHhGRkZGKTtcclxuICB2YXIgbXN3ID0gKHggPj4gMTYpICsgKHkgPj4gMTYpICsgKGxzdyA+PiAxNik7XHJcbiAgcmV0dXJuIChtc3cgPDwgMTYpIHwgKGxzdyAmIDB4RkZGRik7XHJcbn1cclxuXHJcbi8qXHJcbiAqIEJpdHdpc2Ugcm90YXRlIGEgMzItYml0IG51bWJlciB0byB0aGUgbGVmdC5cclxuICovXHJcbmZ1bmN0aW9uIGJpdF9yb2wobnVtLCBjbnQpXHJcbntcclxuICByZXR1cm4gKG51bSA8PCBjbnQpIHwgKG51bSA+Pj4gKDMyIC0gY250KSk7XHJcbn1cclxuXHJcbmlmICh0eXBlb2YobW9kdWxlKSAhPT0gJ3VuZGVmaW5lZCcpIHtcclxuICBtb2R1bGUuZXhwb3J0cyA9IHtcclxuICAgIGI2NF9zaGExOiBiNjRfc2hhMSxcclxuICAgIGhleF9zaGExOiBoZXhfc2hhMVxyXG4gIH1cclxufVxyXG4iLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDEwXG4vL1xuLy8gc3BhbnMuanM6IEphdmFTY3JpcHQgSW50c2V0L0xvY2F0aW9uIHBvcnQuXG4vL1xuXG5cInVzZSBzdHJpY3RcIjtcblxuXG5mdW5jdGlvbiBSYW5nZShtaW4sIG1heClcbntcbiAgICBpZiAodHlwZW9mKG1pbikgIT0gJ251bWJlcicgfHwgdHlwZW9mKG1heCkgIT0gJ251bWJlcicpXG4gICAgICAgIHRocm93ICdCYWQgcmFuZ2UgJyArIG1pbiArICcsJyArIG1heDtcbiAgICB0aGlzLl9taW4gPSBtaW47XG4gICAgdGhpcy5fbWF4ID0gbWF4O1xufVxuXG5SYW5nZS5wcm90b3R5cGUubWluID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuX21pbjtcbn1cblxuUmFuZ2UucHJvdG90eXBlLm1heCA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiB0aGlzLl9tYXg7XG59XG5cblJhbmdlLnByb3RvdHlwZS5jb250YWlucyA9IGZ1bmN0aW9uKHBvcykge1xuICAgIHJldHVybiBwb3MgPj0gdGhpcy5fbWluICYmIHBvcyA8PSB0aGlzLl9tYXg7XG59XG5cblJhbmdlLnByb3RvdHlwZS5pc0NvbnRpZ3VvdXMgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdHJ1ZTtcbn1cblxuUmFuZ2UucHJvdG90eXBlLnJhbmdlcyA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiBbdGhpc107XG59XG5cblJhbmdlLnByb3RvdHlwZS5fcHVzaFJhbmdlcyA9IGZ1bmN0aW9uKHJhbmdlcykge1xuICAgIHJhbmdlcy5wdXNoKHRoaXMpO1xufVxuXG5SYW5nZS5wcm90b3R5cGUudG9TdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gJ1snICsgdGhpcy5fbWluICsgJy0nICsgdGhpcy5fbWF4ICsgJ10nO1xufVxuXG5mdW5jdGlvbiBfQ29tcG91bmQocmFuZ2VzKSB7XG4gICAgdGhpcy5fcmFuZ2VzID0gcmFuZ2VzO1xuICAgIC8vIGFzc2VydCBzb3J0ZWQ/XG59XG5cbl9Db21wb3VuZC5wcm90b3R5cGUubWluID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuX3Jhbmdlc1swXS5taW4oKTtcbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS5tYXggPSBmdW5jdGlvbigpIHtcbiAgICByZXR1cm4gdGhpcy5fcmFuZ2VzW3RoaXMuX3Jhbmdlcy5sZW5ndGggLSAxXS5tYXgoKTtcbn1cblxuX0NvbXBvdW5kLnByb3RvdHlwZS5jb250YWlucyA9IGZ1bmN0aW9uKHBvcykge1xuICAgIC8vIEZJWE1FIGltcGxlbWVudCBic2VhcmNoIGlmIHdlIHVzZSB0aGlzIG11Y2guXG4gICAgZm9yICh2YXIgcyA9IDA7IHMgPCB0aGlzLl9yYW5nZXMubGVuZ3RoOyArK3MpIHtcbiAgICAgICAgaWYgKHRoaXMuX3Jhbmdlc1tzXS5jb250YWlucyhwb3MpKSB7XG4gICAgICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gZmFsc2U7XG59XG5cbl9Db21wb3VuZC5wcm90b3R5cGUuaXNDb250aWd1b3VzID0gZnVuY3Rpb24oKSB7XG4gICAgcmV0dXJuIHRoaXMuX3Jhbmdlcy5sZW5ndGggPiAxO1xufVxuXG5fQ29tcG91bmQucHJvdG90eXBlLnJhbmdlcyA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiB0aGlzLl9yYW5nZXM7XG59XG5cbl9Db21wb3VuZC5wcm90b3R5cGUuX3B1c2hSYW5nZXMgPSBmdW5jdGlvbihyYW5nZXMpIHtcbiAgICBmb3IgKHZhciByaSA9IDA7IHJpIDwgdGhpcy5fcmFuZ2VzLmxlbmd0aDsgKytyaSlcbiAgICAgICAgcmFuZ2VzLnB1c2godGhpcy5fcmFuZ2VzW3JpXSk7XG59XG5cbl9Db21wb3VuZC5wcm90b3R5cGUudG9TdHJpbmcgPSBmdW5jdGlvbigpIHtcbiAgICB2YXIgcyA9ICcnO1xuICAgIGZvciAodmFyIHIgPSAwOyByIDwgdGhpcy5fcmFuZ2VzLmxlbmd0aDsgKytyKSB7XG4gICAgICAgIGlmIChyPjApIHtcbiAgICAgICAgICAgIHMgPSBzICsgJywnO1xuICAgICAgICB9XG4gICAgICAgIHMgPSBzICsgdGhpcy5fcmFuZ2VzW3JdLnRvU3RyaW5nKCk7XG4gICAgfVxuICAgIHJldHVybiBzO1xufVxuXG5mdW5jdGlvbiB1bmlvbihzMCwgczEpIHtcbiAgICBpZiAoISAoczAgaW5zdGFuY2VvZiBBcnJheSkpIHtcbiAgICAgICAgczAgPSBbczBdO1xuICAgICAgICBpZiAoczEpXG4gICAgICAgICAgICBzMC5wdXNoKHMxKTtcbiAgICB9XG5cbiAgICBpZiAoczAubGVuZ3RoID09IDApXG4gICAgICAgIHJldHVybiBudWxsO1xuICAgIGVsc2UgaWYgKHMwLmxlbmd0aCA9PSAxKVxuICAgICAgICByZXR1cm4gczBbMF07XG5cbiAgICB2YXIgcmFuZ2VzID0gW107XG4gICAgZm9yICh2YXIgc2kgPSAwOyBzaSA8IHMwLmxlbmd0aDsgKytzaSlcbiAgICAgICAgczBbc2ldLl9wdXNoUmFuZ2VzKHJhbmdlcyk7XG4gICAgcmFuZ2VzID0gcmFuZ2VzLnNvcnQoX3JhbmdlT3JkZXIpO1xuXG4gICAgdmFyIG9yYW5nZXMgPSBbXTtcbiAgICB2YXIgY3VycmVudCA9IHJhbmdlc1swXTtcbiAgICBjdXJyZW50ID0gbmV3IFJhbmdlKGN1cnJlbnQuX21pbiwgY3VycmVudC5fbWF4KTsgIC8vIENvcHkgbm93IHNvIHdlIGRvbid0IGhhdmUgdG8gbGF0ZXIuXG5cbiAgICBmb3IgKHZhciBpID0gMTsgaSA8IHJhbmdlcy5sZW5ndGg7ICsraSkge1xuICAgICAgICB2YXIgbnh0ID0gcmFuZ2VzW2ldO1xuICAgICAgICBpZiAobnh0Ll9taW4gPiAoY3VycmVudC5fbWF4ICsgMSkpIHtcbiAgICAgICAgICAgIG9yYW5nZXMucHVzaChjdXJyZW50KTtcbiAgICAgICAgICAgIGN1cnJlbnQgPSBuZXcgUmFuZ2Uobnh0Ll9taW4sIG54dC5fbWF4KTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIGlmIChueHQuX21heCA+IGN1cnJlbnQuX21heCkge1xuICAgICAgICAgICAgICAgIGN1cnJlbnQuX21heCA9IG54dC5fbWF4O1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIG9yYW5nZXMucHVzaChjdXJyZW50KTtcblxuICAgIGlmIChvcmFuZ2VzLmxlbmd0aCA9PSAxKSB7XG4gICAgICAgIHJldHVybiBvcmFuZ2VzWzBdO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiBuZXcgX0NvbXBvdW5kKG9yYW5nZXMpO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gaW50ZXJzZWN0aW9uKHMwLCBzMSkge1xuICAgIHZhciByMCA9IHMwLnJhbmdlcygpO1xuICAgIHZhciByMSA9IHMxLnJhbmdlcygpO1xuICAgIHZhciBsMCA9IHIwLmxlbmd0aCwgbDEgPSByMS5sZW5ndGg7XG4gICAgdmFyIGkwID0gMCwgaTEgPSAwO1xuICAgIHZhciBvciA9IFtdO1xuXG4gICAgd2hpbGUgKGkwIDwgbDAgJiYgaTEgPCBsMSkge1xuICAgICAgICB2YXIgczAgPSByMFtpMF0sIHMxID0gcjFbaTFdO1xuICAgICAgICB2YXIgbGFwTWluID0gTWF0aC5tYXgoczAubWluKCksIHMxLm1pbigpKTtcbiAgICAgICAgdmFyIGxhcE1heCA9IE1hdGgubWluKHMwLm1heCgpLCBzMS5tYXgoKSk7XG4gICAgICAgIGlmIChsYXBNYXggPj0gbGFwTWluKSB7XG4gICAgICAgICAgICBvci5wdXNoKG5ldyBSYW5nZShsYXBNaW4sIGxhcE1heCkpO1xuICAgICAgICB9XG4gICAgICAgIGlmIChzMC5tYXgoKSA+IHMxLm1heCgpKSB7XG4gICAgICAgICAgICArK2kxO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgKytpMDtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBpZiAob3IubGVuZ3RoID09IDApIHtcbiAgICAgICAgcmV0dXJuIG51bGw7IC8vIEZJWE1FXG4gICAgfSBlbHNlIGlmIChvci5sZW5ndGggPT0gMSkge1xuICAgICAgICByZXR1cm4gb3JbMF07XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIG5ldyBfQ29tcG91bmQob3IpO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gY292ZXJhZ2Uocykge1xuICAgIHZhciB0b3QgPSAwO1xuICAgIHZhciBybCA9IHMucmFuZ2VzKCk7XG4gICAgZm9yICh2YXIgcmkgPSAwOyByaSA8IHJsLmxlbmd0aDsgKytyaSkge1xuICAgICAgICB2YXIgciA9IHJsW3JpXTtcbiAgICAgICAgdG90ICs9IChyLm1heCgpIC0gci5taW4oKSArIDEpO1xuICAgIH1cbiAgICByZXR1cm4gdG90O1xufVxuXG5cblxuZnVuY3Rpb24gcmFuZ2VPcmRlcihhLCBiKVxue1xuICAgIGlmIChhLm1pbigpIDwgYi5taW4oKSkge1xuICAgICAgICByZXR1cm4gLTE7XG4gICAgfSBlbHNlIGlmIChhLm1pbigpID4gYi5taW4oKSkge1xuICAgICAgICByZXR1cm4gMTtcbiAgICB9IGVsc2UgaWYgKGEubWF4KCkgPCBiLm1heCgpKSB7XG4gICAgICAgIHJldHVybiAtMTtcbiAgICB9IGVsc2UgaWYgKGIubWF4KCkgPiBhLm1heCgpKSB7XG4gICAgICAgIHJldHVybiAxO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiAwO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gX3JhbmdlT3JkZXIoYSwgYilcbntcbiAgICBpZiAoYS5fbWluIDwgYi5fbWluKSB7XG4gICAgICAgIHJldHVybiAtMTtcbiAgICB9IGVsc2UgaWYgKGEuX21pbiA+IGIuX21pbikge1xuICAgICAgICByZXR1cm4gMTtcbiAgICB9IGVsc2UgaWYgKGEuX21heCA8IGIuX21heCkge1xuICAgICAgICByZXR1cm4gLTE7XG4gICAgfSBlbHNlIGlmIChiLl9tYXggPiBhLl9tYXgpIHtcbiAgICAgICAgcmV0dXJuIDE7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIDA7XG4gICAgfVxufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIFJhbmdlOiBSYW5nZSxcbiAgICAgICAgdW5pb246IHVuaW9uLFxuICAgICAgICBpbnRlcnNlY3Rpb246IGludGVyc2VjdGlvbixcbiAgICAgICAgY292ZXJhZ2U6IGNvdmVyYWdlLFxuICAgICAgICByYW5nZU92ZXI6IHJhbmdlT3JkZXIsXG4gICAgICAgIF9yYW5nZU9yZGVyOiBfcmFuZ2VPcmRlclxuICAgIH1cbn0iLCIvKiAtKi0gbW9kZTogamF2YXNjcmlwdDsgYy1iYXNpYy1vZmZzZXQ6IDQ7IGluZGVudC10YWJzLW1vZGU6IG5pbCAtKi0gKi9cblxuLy8gXG4vLyBEYWxsaWFuY2UgR2Vub21lIEV4cGxvcmVyXG4vLyAoYykgVGhvbWFzIERvd24gMjAwNi0yMDEwXG4vL1xuLy8gdXRpbHMuanM6IG9kZHMsIHNvZHMsIGFuZCBlbmRzLlxuLy9cblxuXCJ1c2Ugc3RyaWN0XCI7XG5cbmlmICh0eXBlb2YocmVxdWlyZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgdmFyIHNoYTEgPSByZXF1aXJlKCcuL3NoYTEnKTtcbiAgICB2YXIgYjY0X3NoYTEgPSBzaGExLmI2NF9zaGExO1xufVxuXG52YXIgTlVNX1JFR0VYUCA9IG5ldyBSZWdFeHAoJ1swLTldKycpO1xuXG5mdW5jdGlvbiBzdHJpbmdUb051bWJlcnNBcnJheShzdHIpIHtcbiAgICB2YXIgbnVtcyA9IG5ldyBBcnJheSgpO1xuICAgIHZhciBtO1xuICAgIHdoaWxlIChtID0gTlVNX1JFR0VYUC5leGVjKHN0cikpIHtcbiAgICAgICAgbnVtcy5wdXNoKG1bMF0pO1xuICAgICAgICBzdHI9c3RyLnN1YnN0cmluZyhtLmluZGV4ICsgKG1bMF0ubGVuZ3RoKSk7XG4gICAgfVxuICAgIHJldHVybiBudW1zO1xufVxuXG52YXIgU1RSSUNUX05VTV9SRUdFWFAgPSBuZXcgUmVnRXhwKCdeWzAtOV0rJCcpO1xuXG5mdW5jdGlvbiBzdHJpbmdUb0ludChzdHIpIHtcbiAgICBzdHIgPSBzdHIucmVwbGFjZShuZXcgUmVnRXhwKCcsJywgJ2cnKSwgJycpO1xuICAgIGlmICghU1RSSUNUX05VTV9SRUdFWFAudGVzdChzdHIpKSB7XG4gICAgICAgIHJldHVybiBudWxsO1xuICAgIH1cbiAgICByZXR1cm4gc3RyfDA7XG59XG5cbmZ1bmN0aW9uIHB1c2huZXcoYSwgdikge1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgYS5sZW5ndGg7ICsraSkge1xuICAgICAgICBpZiAoYVtpXSA9PSB2KSB7XG4gICAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICB9XG4gICAgYS5wdXNoKHYpO1xufVxuXG5mdW5jdGlvbiBwdXNobyhvYmosIGssIHYpIHtcbiAgICBpZiAob2JqW2tdKSB7XG4gICAgICAgIG9ialtrXS5wdXNoKHYpO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIG9ialtrXSA9IFt2XTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIHB1c2huZXdvKG9iaiwgaywgdikge1xuICAgIHZhciBhID0gb2JqW2tdO1xuICAgIGlmIChhKSB7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgYS5sZW5ndGg7ICsraSkgeyAgICAvLyBpbmRleE9mIHJlcXVpcmVzIEpTMTYgOi0oLlxuICAgICAgICAgICAgaWYgKGFbaV0gPT0gdikge1xuICAgICAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgICBhLnB1c2godik7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgb2JqW2tdID0gW3ZdO1xuICAgIH1cbn1cblxuXG5mdW5jdGlvbiBwaWNrKGEsIGIsIGMsIGQpXG57XG4gICAgaWYgKGEpIHtcbiAgICAgICAgcmV0dXJuIGE7XG4gICAgfSBlbHNlIGlmIChiKSB7XG4gICAgICAgIHJldHVybiBiO1xuICAgIH0gZWxzZSBpZiAoYykge1xuICAgICAgICByZXR1cm4gYztcbiAgICB9IGVsc2UgaWYgKGQpIHtcbiAgICAgICAgcmV0dXJuIGQ7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBwdXNobmV3KGwsIG8pXG57XG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBsLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIGlmIChsW2ldID09IG8pIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgIH1cbiAgICBsLnB1c2gobyk7XG59XG5cblxuXG5mdW5jdGlvbiBhcnJheUluZGV4T2YoYSwgeCkge1xuICAgIGlmICghYSkge1xuICAgICAgICByZXR1cm4gLTE7XG4gICAgfVxuXG4gICAgZm9yICh2YXIgaSA9IDA7IGkgPCBhLmxlbmd0aDsgKytpKSB7XG4gICAgICAgIGlmIChhW2ldID09PSB4KSB7XG4gICAgICAgICAgICByZXR1cm4gaTtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gLTE7XG59XG5cbmZ1bmN0aW9uIGFycmF5UmVtb3ZlKGEsIHgpIHtcbiAgICB2YXIgaSA9IGFycmF5SW5kZXhPZihhLCB4KTtcbiAgICBpZiAoaSA+PSAwKSB7XG4gICAgICAgIGEuc3BsaWNlKGksIDEpO1xuICAgICAgICByZXR1cm4gdHJ1ZTtcbiAgICB9XG4gICAgcmV0dXJuIGZhbHNlO1xufVxuXG4vL1xuLy8gRE9NIHV0aWxpdGllc1xuLy9cblxuXG5mdW5jdGlvbiBtYWtlRWxlbWVudCh0YWcsIGNoaWxkcmVuLCBhdHRyaWJzLCBzdHlsZXMpXG57XG4gICAgdmFyIGVsZSA9IGRvY3VtZW50LmNyZWF0ZUVsZW1lbnQodGFnKTtcbiAgICBpZiAoY2hpbGRyZW4pIHtcbiAgICAgICAgaWYgKCEgKGNoaWxkcmVuIGluc3RhbmNlb2YgQXJyYXkpKSB7XG4gICAgICAgICAgICBjaGlsZHJlbiA9IFtjaGlsZHJlbl07XG4gICAgICAgIH1cbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBjaGlsZHJlbi5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGMgPSBjaGlsZHJlbltpXTtcbiAgICAgICAgICAgIGlmIChjKSB7XG4gICAgICAgICAgICAgICAgaWYgKHR5cGVvZiBjID09ICdzdHJpbmcnKSB7XG4gICAgICAgICAgICAgICAgICAgIGMgPSBkb2N1bWVudC5jcmVhdGVUZXh0Tm9kZShjKTtcbiAgICAgICAgICAgICAgICB9IGVsc2UgaWYgKHR5cGVvZiBjID09ICdudW1iZXInKSB7XG4gICAgICAgICAgICAgICAgICAgIGMgPSBkb2N1bWVudC5jcmVhdGVUZXh0Tm9kZSgnJyArIGMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBlbGUuYXBwZW5kQ2hpbGQoYyk7XG4gICAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICB9XG4gICAgXG4gICAgaWYgKGF0dHJpYnMpIHtcbiAgICAgICAgZm9yICh2YXIgbCBpbiBhdHRyaWJzKSB7XG4gICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgIGVsZVtsXSA9IGF0dHJpYnNbbF07XG4gICAgICAgICAgICB9IGNhdGNoIChlKSB7XG4gICAgICAgICAgICAgICAgY29uc29sZS5sb2coJ2Vycm9yIHNldHRpbmcgJyArIGwpO1xuICAgICAgICAgICAgICAgIHRocm93KGUpO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgfVxuICAgIGlmIChzdHlsZXMpIHtcbiAgICAgICAgZm9yICh2YXIgbCBpbiBzdHlsZXMpIHtcbiAgICAgICAgICAgIGVsZS5zdHlsZVtsXSA9IHN0eWxlc1tsXTtcbiAgICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gZWxlO1xufVxuXG5mdW5jdGlvbiBtYWtlRWxlbWVudE5TKG5hbWVzcGFjZSwgdGFnLCBjaGlsZHJlbiwgYXR0cmlicylcbntcbiAgICB2YXIgZWxlID0gZG9jdW1lbnQuY3JlYXRlRWxlbWVudE5TKG5hbWVzcGFjZSwgdGFnKTtcbiAgICBpZiAoY2hpbGRyZW4pIHtcbiAgICAgICAgaWYgKCEgKGNoaWxkcmVuIGluc3RhbmNlb2YgQXJyYXkpKSB7XG4gICAgICAgICAgICBjaGlsZHJlbiA9IFtjaGlsZHJlbl07XG4gICAgICAgIH1cbiAgICAgICAgZm9yICh2YXIgaSA9IDA7IGkgPCBjaGlsZHJlbi5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGMgPSBjaGlsZHJlbltpXTtcbiAgICAgICAgICAgIGlmICh0eXBlb2YgYyA9PSAnc3RyaW5nJykge1xuICAgICAgICAgICAgICAgIGMgPSBkb2N1bWVudC5jcmVhdGVUZXh0Tm9kZShjKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIGVsZS5hcHBlbmRDaGlsZChjKTtcbiAgICAgICAgfVxuICAgIH1cbiAgICBcbiAgICBzZXRBdHRycyhlbGUsIGF0dHJpYnMpO1xuICAgIHJldHVybiBlbGU7XG59XG5cbnZhciBhdHRyX25hbWVfY2FjaGUgPSB7fTtcblxuZnVuY3Rpb24gc2V0QXR0cihub2RlLCBrZXksIHZhbHVlKVxue1xuICAgIHZhciBhdHRyID0gYXR0cl9uYW1lX2NhY2hlW2tleV07XG4gICAgaWYgKCFhdHRyKSB7XG4gICAgICAgIHZhciBfYXR0ciA9ICcnO1xuICAgICAgICBmb3IgKHZhciBjID0gMDsgYyA8IGtleS5sZW5ndGg7ICsrYykge1xuICAgICAgICAgICAgdmFyIGNjID0ga2V5LnN1YnN0cmluZyhjLCBjKzEpO1xuICAgICAgICAgICAgdmFyIGxjYyA9IGNjLnRvTG93ZXJDYXNlKCk7XG4gICAgICAgICAgICBpZiAobGNjICE9IGNjKSB7XG4gICAgICAgICAgICAgICAgX2F0dHIgPSBfYXR0ciArICctJyArIGxjYztcbiAgICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICAgICAgX2F0dHIgPSBfYXR0ciArIGNjO1xuICAgICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGF0dHJfbmFtZV9jYWNoZVtrZXldID0gX2F0dHI7XG4gICAgICAgIGF0dHIgPSBfYXR0cjtcbiAgICB9XG4gICAgbm9kZS5zZXRBdHRyaWJ1dGUoYXR0ciwgdmFsdWUpO1xufVxuXG5mdW5jdGlvbiBzZXRBdHRycyhub2RlLCBhdHRyaWJzKVxue1xuICAgIGlmIChhdHRyaWJzKSB7XG4gICAgICAgIGZvciAodmFyIGwgaW4gYXR0cmlicykge1xuICAgICAgICAgICAgc2V0QXR0cihub2RlLCBsLCBhdHRyaWJzW2xdKTtcbiAgICAgICAgfVxuICAgIH1cbn1cblxuXG5cbmZ1bmN0aW9uIHJlbW92ZUNoaWxkcmVuKG5vZGUpXG57XG4gICAgaWYgKCFub2RlIHx8ICFub2RlLmNoaWxkTm9kZXMpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIHdoaWxlIChub2RlLmNoaWxkTm9kZXMubGVuZ3RoID4gMCkge1xuICAgICAgICBub2RlLnJlbW92ZUNoaWxkKG5vZGUuZmlyc3RDaGlsZCk7XG4gICAgfVxufVxuXG5cblxuLy9cbi8vIFdBUk5JTkc6IG5vdCBmb3IgZ2VuZXJhbCB1c2UhXG4vL1xuXG5mdW5jdGlvbiBtaW5pSlNPTmlmeShvLCBleGMpIHtcbiAgICBpZiAodHlwZW9mIG8gPT09ICd1bmRlZmluZWQnKSB7XG4gICAgICAgIHJldHVybiAndW5kZWZpbmVkJztcbiAgICB9IGVsc2UgaWYgKG8gPT0gbnVsbCkge1xuICAgICAgICByZXR1cm4gJ251bGwnO1xuICAgIH0gZWxzZSBpZiAodHlwZW9mIG8gPT0gJ3N0cmluZycpIHtcbiAgICAgICAgcmV0dXJuIFwiJ1wiICsgbyArIFwiJ1wiO1xuICAgIH0gZWxzZSBpZiAodHlwZW9mIG8gPT0gJ251bWJlcicpIHtcbiAgICAgICAgcmV0dXJuIFwiXCIgKyBvO1xuICAgIH0gZWxzZSBpZiAodHlwZW9mIG8gPT0gJ2Jvb2xlYW4nKSB7XG4gICAgICAgIHJldHVybiBcIlwiICsgbztcbiAgICB9IGVsc2UgaWYgKHR5cGVvZiBvID09ICdvYmplY3QnKSB7XG4gICAgICAgIGlmIChvIGluc3RhbmNlb2YgQXJyYXkpIHtcbiAgICAgICAgICAgIHZhciBzID0gbnVsbDtcbiAgICAgICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgby5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgICAgIHMgPSAocyA9PSBudWxsID8gJycgOiAocyArICcsICcpKSArIG1pbmlKU09OaWZ5KG9baV0sIGV4Yyk7XG4gICAgICAgICAgICB9XG4gICAgICAgICAgICByZXR1cm4gJ1snICsgKHM/czonJykgKyAnXSc7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBleGMgPSBleGMgfHwge307XG4gICAgICAgICAgICB2YXIgcyA9IG51bGw7XG4gICAgICAgICAgICBmb3IgKHZhciBrIGluIG8pIHtcbiAgICAgICAgICAgICAgICBpZiAoZXhjW2tdKVxuICAgICAgICAgICAgICAgICAgICBjb250aW51ZTtcbiAgICAgICAgICAgICAgICBpZiAoayAhPSB1bmRlZmluZWQgJiYgdHlwZW9mKG9ba10pICE9ICdmdW5jdGlvbicpIHtcbiAgICAgICAgICAgICAgICAgICAgcyA9IChzID09IG51bGwgPyAnJyA6IChzICsgJywgJykpICsgayArICc6ICcgKyBtaW5pSlNPTmlmeShvW2tdLCBleGMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybiAneycgKyAocz9zOicnKSArICd9JztcbiAgICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICAgIHJldHVybiAodHlwZW9mIG8pO1xuICAgIH1cbn1cblxuZnVuY3Rpb24gc2hhbGxvd0NvcHkobykge1xuICAgIHZhciBuID0ge307XG4gICAgZm9yICh2YXIgayBpbiBvKSB7XG4gICAgICAgIG5ba10gPSBvW2tdO1xuICAgIH1cbiAgICByZXR1cm4gbjtcbn1cblxuZnVuY3Rpb24gT2JzZXJ2ZWQoeCkge1xuICAgIHRoaXMudmFsdWUgPSB4O1xuICAgIHRoaXMubGlzdGVuZXJzID0gW107XG59XG5cbk9ic2VydmVkLnByb3RvdHlwZS5hZGRMaXN0ZW5lciA9IGZ1bmN0aW9uKGYpIHtcbiAgICB0aGlzLmxpc3RlbmVycy5wdXNoKGYpO1xufVxuXG5PYnNlcnZlZC5wcm90b3R5cGUuYWRkTGlzdGVuZXJBbmRGaXJlID0gZnVuY3Rpb24oZikge1xuICAgIHRoaXMubGlzdGVuZXJzLnB1c2goZik7XG4gICAgZih0aGlzLnZhbHVlKTtcbn1cblxuT2JzZXJ2ZWQucHJvdG90eXBlLnJlbW92ZUxpc3RlbmVyID0gZnVuY3Rpb24oZikge1xuICAgIGFycmF5UmVtb3ZlKHRoaXMubGlzdGVuZXJzLCBmKTtcbn1cblxuT2JzZXJ2ZWQucHJvdG90eXBlLmdldCA9IGZ1bmN0aW9uKCkge1xuICAgIHJldHVybiB0aGlzLnZhbHVlO1xufVxuXG5PYnNlcnZlZC5wcm90b3R5cGUuc2V0ID0gZnVuY3Rpb24oeCkge1xuICAgIHRoaXMudmFsdWUgPSB4O1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5saXN0ZW5lcnMubGVuZ3RoOyArK2kpIHtcbiAgICAgICAgdGhpcy5saXN0ZW5lcnNbaV0oeCk7XG4gICAgfVxufVxuXG5mdW5jdGlvbiBBd2FpdGVkKCkge1xuICAgIHRoaXMucXVldWUgPSBbXTtcbn1cblxuQXdhaXRlZC5wcm90b3R5cGUucHJvdmlkZSA9IGZ1bmN0aW9uKHgpIHtcbiAgICBpZiAodGhpcy5yZXMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBcIlJlc291cmNlIGhhcyBhbHJlYWR5IGJlZW4gcHJvdmlkZWQuXCI7XG4gICAgfVxuXG4gICAgdGhpcy5yZXMgPSB4O1xuICAgIGZvciAodmFyIGkgPSAwOyBpIDwgdGhpcy5xdWV1ZS5sZW5ndGg7ICsraSkge1xuICAgICAgICB0aGlzLnF1ZXVlW2ldKHgpO1xuICAgIH1cbiAgICB0aGlzLnF1ZXVlID0gbnVsbDsgICAvLyBhdm9pZCBsZWFraW5nIGNsb3N1cmVzLlxufVxuXG5Bd2FpdGVkLnByb3RvdHlwZS5hd2FpdCA9IGZ1bmN0aW9uKGYpIHtcbiAgICBpZiAodGhpcy5yZXMgIT09IHVuZGVmaW5lZCkge1xuICAgICAgICBmKHRoaXMucmVzKTtcbiAgICAgICAgcmV0dXJuIHRoaXMucmVzO1xuICAgIH0gZWxzZSB7XG4gICAgICAgIHRoaXMucXVldWUucHVzaChmKTtcbiAgICB9XG59XG5cbnZhciBfX2RhbGxpYW5jZV9zYWx0U2VlZCA9IDA7XG5cbmZ1bmN0aW9uIHNhbHRVUkwodXJsKSB7XG4gICAgcmV0dXJuIHVybCArICc/c2FsdD0nICsgYjY0X3NoYTEoJycgKyBEYXRlLm5vdygpICsgJywnICsgKCsrX19kYWxsaWFuY2Vfc2FsdFNlZWQpKTtcbn1cblxuZnVuY3Rpb24gdGV4dFhIUih1cmwsIGNhbGxiYWNrLCBvcHRzKSB7XG4gICAgaWYgKG9wdHMuc2FsdCkgXG4gICAgICAgIHVybCA9IHNhbHRVUkwodXJsKTtcblxuICAgIHZhciByZXEgPSBuZXcgWE1MSHR0cFJlcXVlc3QoKTtcbiAgICByZXEub25yZWFkeXN0YXRlY2hhbmdlID0gZnVuY3Rpb24oKSB7XG4gICAgXHRpZiAocmVxLnJlYWR5U3RhdGUgPT0gNCkge1xuICAgIFx0ICAgIGlmIChyZXEuc3RhdHVzID49IDMwMCkge1xuICAgIFx0XHQgICAgY2FsbGJhY2sobnVsbCwgJ0Vycm9yIGNvZGUgJyArIHJlcS5zdGF0dXMpO1xuICAgIFx0ICAgIH0gZWxzZSB7XG4gICAgXHRcdCAgICBjYWxsYmFjayhyZXEucmVzcG9uc2VUZXh0KTtcbiAgICBcdCAgICB9XG4gICAgXHR9XG4gICAgfTtcbiAgICBcbiAgICByZXEub3BlbignR0VUJywgdXJsLCB0cnVlKTtcbiAgICByZXEucmVzcG9uc2VUeXBlID0gJ3RleHQnO1xuXG4gICAgaWYgKG9wdHMgJiYgb3B0cy5jcmVkZW50aWFscykge1xuICAgICAgICByZXEud2l0aENyZWRlbnRpYWxzID0gdHJ1ZTtcbiAgICB9XG4gICAgcmVxLnNlbmQoJycpO1xufVxuXG5mdW5jdGlvbiByZWxhdGl2ZVVSTChiYXNlLCByZWwpIHtcbiAgICAvLyBGSVhNRSBxdWl0ZSBuYWl2ZSAtLSBnb29kIGVub3VnaCBmb3IgdHJhY2todWJzP1xuXG4gICAgaWYgKHJlbC5pbmRleE9mKCdodHRwOicpID09IDAgfHwgcmVsLmluZGV4T2YoJ2h0dHBzOicpID09IDApIHtcbiAgICAgICAgcmV0dXJuIHJlbDtcbiAgICB9XG5cbiAgICB2YXIgbGkgPSBiYXNlLmxhc3RJbmRleE9mKCcvJyk7XG4gICAgaWYgKGxpID49IDApIHtcbiAgICAgICAgcmV0dXJuIGJhc2Uuc3Vic3RyKDAsIGxpICsgMSkgKyByZWw7XG4gICAgfSBlbHNlIHtcbiAgICAgICAgcmV0dXJuIHJlbDtcbiAgICB9XG59XG5cbnZhciBBTUlOT19BQ0lEX1RSQU5TTEFUSU9OID0ge1xuICAgICdUVFQnOiAnRicsXG4gICAgJ1RUQyc6ICdGJyxcbiAgICAnVFRBJzogJ0wnLFxuICAgICdUVEcnOiAnTCcsXG4gICAgJ0NUVCc6ICdMJyxcbiAgICAnQ1RDJzogJ0wnLFxuICAgICdDVEEnOiAnTCcsXG4gICAgJ0NURyc6ICdMJyxcbiAgICAnQVRUJzogJ0knLFxuICAgICdBVEMnOiAnSScsXG4gICAgJ0FUQSc6ICdJJyxcbiAgICAnQVRHJzogJ00nLFxuICAgICdHVFQnOiAnVicsXG4gICAgJ0dUQyc6ICdWJyxcbiAgICAnR1RBJzogJ1YnLFxuICAgICdHVEcnOiAnVicsXG4gICAgJ1RDVCc6ICdTJyxcbiAgICAnVENDJzogJ1MnLFxuICAgICdUQ0EnOiAnUycsXG4gICAgJ1RDRyc6ICdTJyxcbiAgICAnQ0NUJzogJ1AnLFxuICAgICdDQ0MnOiAnUCcsXG4gICAgJ0NDQSc6ICdQJyxcbiAgICAnQ0NHJzogJ1AnLFxuICAgICdBQ1QnOiAnVCcsXG4gICAgJ0FDQyc6ICdUJyxcbiAgICAnQUNBJzogJ1QnLFxuICAgICdBQ0cnOiAnVCcsXG4gICAgJ0dDVCc6ICdBJyxcbiAgICAnR0NDJzogJ0EnLFxuICAgICdHQ0EnOiAnQScsXG4gICAgJ0dDRyc6ICdBJyxcbiAgICAnVEFUJzogJ1knLFxuICAgICdUQUMnOiAnWScsXG4gICAgJ1RBQSc6ICcqJywgIC8vIHN0b3BcbiAgICAnVEFHJzogJyonLCAgLy8gc3RvcFxuICAgICdDQVQnOiAnSCcsXG4gICAgJ0NBQyc6ICdIJyxcbiAgICAnQ0FBJzogJ1EnLFxuICAgICdDQUcnOiAnUScsXG4gICAgJ0FBVCc6ICdOJyxcbiAgICAnQUFDJzogJ04nLFxuICAgICdBQUEnOiAnSycsXG4gICAgJ0FBRyc6ICdLJyxcbiAgICAnR0FUJzogJ0QnLFxuICAgICdHQUMnOiAnRCcsXG4gICAgJ0dBQSc6ICdFJyxcbiAgICAnR0FHJzogJ0UnLFxuICAgICdUR1QnOiAnQycsXG4gICAgJ1RHQyc6ICdDJyxcbiAgICAnVEdBJzogJyonLCAgLy8gc3RvcFxuICAgICdUR0cnOiAnVycsXG4gICAgJ0NHVCc6ICdSJyxcbiAgICAnQ0dDJzogJ1InLFxuICAgICdDR0EnOiAnUicsXG4gICAgJ0NHRyc6ICdSJyxcbiAgICAnQUdUJzogJ1MnLFxuICAgICdBR0MnOiAnUycsXG4gICAgJ0FHQSc6ICdSJyxcbiAgICAnQUdHJzogJ1InLFxuICAgICdHR1QnOiAnRycsXG4gICAgJ0dHQyc6ICdHJyxcbiAgICAnR0dBJzogJ0cnLFxuICAgICdHR0cnOiAnRydcbn1cblxuZnVuY3Rpb24gcmVzb2x2ZVVybFRvUGFnZShyZWwpIHtcbiAgICByZXR1cm4gbWFrZUVsZW1lbnQoJ2EnLCBudWxsLCB7aHJlZjogcmVsfSkuaHJlZjtcbn1cblxuLy9cbi8vIE1pc3NpbmcgQVBJc1xuLy8gXG5cbmlmICghKCd0cmltJyBpbiBTdHJpbmcucHJvdG90eXBlKSkge1xuICAgIFN0cmluZy5wcm90b3R5cGUudHJpbSA9IGZ1bmN0aW9uKCkge1xuICAgICAgICByZXR1cm4gdGhpcy5yZXBsYWNlKC9eXFxzKy8sICcnKS5yZXBsYWNlKC9cXHMrJC8sICcnKTtcbiAgICB9O1xufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gICAgbW9kdWxlLmV4cG9ydHMgPSB7XG4gICAgICAgIHRleHRYSFI6IHRleHRYSFIsXG4gICAgICAgIHJlbGF0aXZlVVJMOiByZWxhdGl2ZVVSTCxcbiAgICAgICAgcmVzb2x2ZVVybFRvUGFnZTogcmVzb2x2ZVVybFRvUGFnZSxcbiAgICAgICAgc2hhbGxvd0NvcHk6IHNoYWxsb3dDb3B5LFxuICAgICAgICBwdXNobzogcHVzaG8sXG4gICAgICAgIHB1c2huZXc6IHB1c2huZXcsXG4gICAgICAgIHB1c2huZXdvOiBwdXNobmV3byxcbiAgICAgICAgYXJyYXlJbmRleE9mOiBhcnJheUluZGV4T2YsXG4gICAgICAgIHBpY2s6IHBpY2ssXG5cbiAgICAgICAgbWFrZUVsZW1lbnQ6IG1ha2VFbGVtZW50LFxuICAgICAgICBtYWtlRWxlbWVudE5TOiBtYWtlRWxlbWVudE5TLFxuICAgICAgICByZW1vdmVDaGlsZHJlbjogcmVtb3ZlQ2hpbGRyZW4sXG5cbiAgICAgICAgbWluaUpTT05pZnk6IG1pbmlKU09OaWZ5LFxuXG4gICAgICAgIE9ic2VydmVkOiBPYnNlcnZlZCxcbiAgICAgICAgQXdhaXRlZDogQXdhaXRlZCxcblxuICAgICAgICBBTUlOT19BQ0lEX1RSQU5TTEFUSU9OOiBBTUlOT19BQ0lEX1RSQU5TTEFUSU9OXG4gICAgfVxufVxuIiwiLyogLSotIG1vZGU6IGphdmFzY3JpcHQ7IGMtYmFzaWMtb2Zmc2V0OiA0OyBpbmRlbnQtdGFicy1tb2RlOiBuaWwgLSotICovXG5cbi8vIFxuLy8gSmF2YXNjcmlwdCBaTGliXG4vLyBCeSBUaG9tYXMgRG93biAyMDEwLTIwMTFcbi8vXG4vLyBCYXNlZCB2ZXJ5IGhlYXZpbHkgb24gcG9ydGlvbnMgb2YganpsaWIgKGJ5IHltbmtAamNyYWZ0LmNvbSksIHdobyBpblxuLy8gdHVybiBjcmVkaXRzIEplYW4tbG91cCBHYWlsbHkgYW5kIE1hcmsgQWRsZXIgZm9yIHRoZSBvcmlnaW5hbCB6bGliIGNvZGUuXG4vL1xuLy8gaW5mbGF0ZS5qczogWkxpYiBpbmZsYXRlIGNvZGVcbi8vXG5cbi8vXG4vLyBTaGFyZWQgY29uc3RhbnRzXG4vL1xuXG52YXIgTUFYX1dCSVRTPTE1OyAvLyAzMksgTFo3NyB3aW5kb3dcbnZhciBERUZfV0JJVFM9TUFYX1dCSVRTO1xudmFyIE1BWF9NRU1fTEVWRUw9OTtcbnZhciBNQU5ZPTE0NDA7XG52YXIgQk1BWCA9IDE1O1xuXG4vLyBwcmVzZXQgZGljdGlvbmFyeSBmbGFnIGluIHpsaWIgaGVhZGVyXG52YXIgUFJFU0VUX0RJQ1Q9MHgyMDtcblxudmFyIFpfTk9fRkxVU0g9MDtcbnZhciBaX1BBUlRJQUxfRkxVU0g9MTtcbnZhciBaX1NZTkNfRkxVU0g9MjtcbnZhciBaX0ZVTExfRkxVU0g9MztcbnZhciBaX0ZJTklTSD00O1xuXG52YXIgWl9ERUZMQVRFRD04O1xuXG52YXIgWl9PSz0wO1xudmFyIFpfU1RSRUFNX0VORD0xO1xudmFyIFpfTkVFRF9ESUNUPTI7XG52YXIgWl9FUlJOTz0tMTtcbnZhciBaX1NUUkVBTV9FUlJPUj0tMjtcbnZhciBaX0RBVEFfRVJST1I9LTM7XG52YXIgWl9NRU1fRVJST1I9LTQ7XG52YXIgWl9CVUZfRVJST1I9LTU7XG52YXIgWl9WRVJTSU9OX0VSUk9SPS02O1xuXG52YXIgTUVUSE9EPTA7ICAgLy8gd2FpdGluZyBmb3IgbWV0aG9kIGJ5dGVcbnZhciBGTEFHPTE7ICAgICAvLyB3YWl0aW5nIGZvciBmbGFnIGJ5dGVcbnZhciBESUNUND0yOyAgICAvLyBmb3VyIGRpY3Rpb25hcnkgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBESUNUMz0zOyAgICAvLyB0aHJlZSBkaWN0aW9uYXJ5IGNoZWNrIGJ5dGVzIHRvIGdvXG52YXIgRElDVDI9NDsgICAgLy8gdHdvIGRpY3Rpb25hcnkgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBESUNUMT01OyAgICAvLyBvbmUgZGljdGlvbmFyeSBjaGVjayBieXRlIHRvIGdvXG52YXIgRElDVDA9NjsgICAgLy8gd2FpdGluZyBmb3IgaW5mbGF0ZVNldERpY3Rpb25hcnlcbnZhciBCTE9DS1M9NzsgICAvLyBkZWNvbXByZXNzaW5nIGJsb2Nrc1xudmFyIENIRUNLND04OyAgIC8vIGZvdXIgY2hlY2sgYnl0ZXMgdG8gZ29cbnZhciBDSEVDSzM9OTsgICAvLyB0aHJlZSBjaGVjayBieXRlcyB0byBnb1xudmFyIENIRUNLMj0xMDsgIC8vIHR3byBjaGVjayBieXRlcyB0byBnb1xudmFyIENIRUNLMT0xMTsgIC8vIG9uZSBjaGVjayBieXRlIHRvIGdvXG52YXIgRE9ORT0xMjsgICAgLy8gZmluaXNoZWQgY2hlY2ssIGRvbmVcbnZhciBCQUQ9MTM7ICAgICAvLyBnb3QgYW4gZXJyb3ItLXN0YXkgaGVyZVxuXG52YXIgaW5mbGF0ZV9tYXNrID0gWzB4MDAwMDAwMDAsIDB4MDAwMDAwMDEsIDB4MDAwMDAwMDMsIDB4MDAwMDAwMDcsIDB4MDAwMDAwMGYsIDB4MDAwMDAwMWYsIDB4MDAwMDAwM2YsIDB4MDAwMDAwN2YsIDB4MDAwMDAwZmYsIDB4MDAwMDAxZmYsIDB4MDAwMDAzZmYsIDB4MDAwMDA3ZmYsIDB4MDAwMDBmZmYsIDB4MDAwMDFmZmYsIDB4MDAwMDNmZmYsIDB4MDAwMDdmZmYsIDB4MDAwMGZmZmZdO1xuXG52YXIgSUJfVFlQRT0wOyAgLy8gZ2V0IHR5cGUgYml0cyAoMywgaW5jbHVkaW5nIGVuZCBiaXQpXG52YXIgSUJfTEVOUz0xOyAgLy8gZ2V0IGxlbmd0aHMgZm9yIHN0b3JlZFxudmFyIElCX1NUT1JFRD0yOy8vIHByb2Nlc3Npbmcgc3RvcmVkIGJsb2NrXG52YXIgSUJfVEFCTEU9MzsgLy8gZ2V0IHRhYmxlIGxlbmd0aHNcbnZhciBJQl9CVFJFRT00OyAvLyBnZXQgYml0IGxlbmd0aHMgdHJlZSBmb3IgYSBkeW5hbWljIGJsb2NrXG52YXIgSUJfRFRSRUU9NTsgLy8gZ2V0IGxlbmd0aCwgZGlzdGFuY2UgdHJlZXMgZm9yIGEgZHluYW1pYyBibG9ja1xudmFyIElCX0NPREVTPTY7IC8vIHByb2Nlc3NpbmcgZml4ZWQgb3IgZHluYW1pYyBibG9ja1xudmFyIElCX0RSWT03OyAgIC8vIG91dHB1dCByZW1haW5pbmcgd2luZG93IGJ5dGVzXG52YXIgSUJfRE9ORT04OyAgLy8gZmluaXNoZWQgbGFzdCBibG9jaywgZG9uZVxudmFyIElCX0JBRD05OyAgIC8vIG90IGEgZGF0YSBlcnJvci0tc3R1Y2sgaGVyZVxuXG52YXIgZml4ZWRfYmwgPSA5O1xudmFyIGZpeGVkX2JkID0gNTtcblxudmFyIGZpeGVkX3RsID0gW1xuICAgIDk2LDcsMjU2LCAwLDgsODAsIDAsOCwxNiwgODQsOCwxMTUsXG4gICAgODIsNywzMSwgMCw4LDExMiwgMCw4LDQ4LCAwLDksMTkyLFxuICAgIDgwLDcsMTAsIDAsOCw5NiwgMCw4LDMyLCAwLDksMTYwLFxuICAgIDAsOCwwLCAwLDgsMTI4LCAwLDgsNjQsIDAsOSwyMjQsXG4gICAgODAsNyw2LCAwLDgsODgsIDAsOCwyNCwgMCw5LDE0NCxcbiAgICA4Myw3LDU5LCAwLDgsMTIwLCAwLDgsNTYsIDAsOSwyMDgsXG4gICAgODEsNywxNywgMCw4LDEwNCwgMCw4LDQwLCAwLDksMTc2LFxuICAgIDAsOCw4LCAwLDgsMTM2LCAwLDgsNzIsIDAsOSwyNDAsXG4gICAgODAsNyw0LCAwLDgsODQsIDAsOCwyMCwgODUsOCwyMjcsXG4gICAgODMsNyw0MywgMCw4LDExNiwgMCw4LDUyLCAwLDksMjAwLFxuICAgIDgxLDcsMTMsIDAsOCwxMDAsIDAsOCwzNiwgMCw5LDE2OCxcbiAgICAwLDgsNCwgMCw4LDEzMiwgMCw4LDY4LCAwLDksMjMyLFxuICAgIDgwLDcsOCwgMCw4LDkyLCAwLDgsMjgsIDAsOSwxNTIsXG4gICAgODQsNyw4MywgMCw4LDEyNCwgMCw4LDYwLCAwLDksMjE2LFxuICAgIDgyLDcsMjMsIDAsOCwxMDgsIDAsOCw0NCwgMCw5LDE4NCxcbiAgICAwLDgsMTIsIDAsOCwxNDAsIDAsOCw3NiwgMCw5LDI0OCxcbiAgICA4MCw3LDMsIDAsOCw4MiwgMCw4LDE4LCA4NSw4LDE2MyxcbiAgICA4Myw3LDM1LCAwLDgsMTE0LCAwLDgsNTAsIDAsOSwxOTYsXG4gICAgODEsNywxMSwgMCw4LDk4LCAwLDgsMzQsIDAsOSwxNjQsXG4gICAgMCw4LDIsIDAsOCwxMzAsIDAsOCw2NiwgMCw5LDIyOCxcbiAgICA4MCw3LDcsIDAsOCw5MCwgMCw4LDI2LCAwLDksMTQ4LFxuICAgIDg0LDcsNjcsIDAsOCwxMjIsIDAsOCw1OCwgMCw5LDIxMixcbiAgICA4Miw3LDE5LCAwLDgsMTA2LCAwLDgsNDIsIDAsOSwxODAsXG4gICAgMCw4LDEwLCAwLDgsMTM4LCAwLDgsNzQsIDAsOSwyNDQsXG4gICAgODAsNyw1LCAwLDgsODYsIDAsOCwyMiwgMTkyLDgsMCxcbiAgICA4Myw3LDUxLCAwLDgsMTE4LCAwLDgsNTQsIDAsOSwyMDQsXG4gICAgODEsNywxNSwgMCw4LDEwMiwgMCw4LDM4LCAwLDksMTcyLFxuICAgIDAsOCw2LCAwLDgsMTM0LCAwLDgsNzAsIDAsOSwyMzYsXG4gICAgODAsNyw5LCAwLDgsOTQsIDAsOCwzMCwgMCw5LDE1NixcbiAgICA4NCw3LDk5LCAwLDgsMTI2LCAwLDgsNjIsIDAsOSwyMjAsXG4gICAgODIsNywyNywgMCw4LDExMCwgMCw4LDQ2LCAwLDksMTg4LFxuICAgIDAsOCwxNCwgMCw4LDE0MiwgMCw4LDc4LCAwLDksMjUyLFxuICAgIDk2LDcsMjU2LCAwLDgsODEsIDAsOCwxNywgODUsOCwxMzEsXG4gICAgODIsNywzMSwgMCw4LDExMywgMCw4LDQ5LCAwLDksMTk0LFxuICAgIDgwLDcsMTAsIDAsOCw5NywgMCw4LDMzLCAwLDksMTYyLFxuICAgIDAsOCwxLCAwLDgsMTI5LCAwLDgsNjUsIDAsOSwyMjYsXG4gICAgODAsNyw2LCAwLDgsODksIDAsOCwyNSwgMCw5LDE0NixcbiAgICA4Myw3LDU5LCAwLDgsMTIxLCAwLDgsNTcsIDAsOSwyMTAsXG4gICAgODEsNywxNywgMCw4LDEwNSwgMCw4LDQxLCAwLDksMTc4LFxuICAgIDAsOCw5LCAwLDgsMTM3LCAwLDgsNzMsIDAsOSwyNDIsXG4gICAgODAsNyw0LCAwLDgsODUsIDAsOCwyMSwgODAsOCwyNTgsXG4gICAgODMsNyw0MywgMCw4LDExNywgMCw4LDUzLCAwLDksMjAyLFxuICAgIDgxLDcsMTMsIDAsOCwxMDEsIDAsOCwzNywgMCw5LDE3MCxcbiAgICAwLDgsNSwgMCw4LDEzMywgMCw4LDY5LCAwLDksMjM0LFxuICAgIDgwLDcsOCwgMCw4LDkzLCAwLDgsMjksIDAsOSwxNTQsXG4gICAgODQsNyw4MywgMCw4LDEyNSwgMCw4LDYxLCAwLDksMjE4LFxuICAgIDgyLDcsMjMsIDAsOCwxMDksIDAsOCw0NSwgMCw5LDE4NixcbiAgICAwLDgsMTMsIDAsOCwxNDEsIDAsOCw3NywgMCw5LDI1MCxcbiAgICA4MCw3LDMsIDAsOCw4MywgMCw4LDE5LCA4NSw4LDE5NSxcbiAgICA4Myw3LDM1LCAwLDgsMTE1LCAwLDgsNTEsIDAsOSwxOTgsXG4gICAgODEsNywxMSwgMCw4LDk5LCAwLDgsMzUsIDAsOSwxNjYsXG4gICAgMCw4LDMsIDAsOCwxMzEsIDAsOCw2NywgMCw5LDIzMCxcbiAgICA4MCw3LDcsIDAsOCw5MSwgMCw4LDI3LCAwLDksMTUwLFxuICAgIDg0LDcsNjcsIDAsOCwxMjMsIDAsOCw1OSwgMCw5LDIxNCxcbiAgICA4Miw3LDE5LCAwLDgsMTA3LCAwLDgsNDMsIDAsOSwxODIsXG4gICAgMCw4LDExLCAwLDgsMTM5LCAwLDgsNzUsIDAsOSwyNDYsXG4gICAgODAsNyw1LCAwLDgsODcsIDAsOCwyMywgMTkyLDgsMCxcbiAgICA4Myw3LDUxLCAwLDgsMTE5LCAwLDgsNTUsIDAsOSwyMDYsXG4gICAgODEsNywxNSwgMCw4LDEwMywgMCw4LDM5LCAwLDksMTc0LFxuICAgIDAsOCw3LCAwLDgsMTM1LCAwLDgsNzEsIDAsOSwyMzgsXG4gICAgODAsNyw5LCAwLDgsOTUsIDAsOCwzMSwgMCw5LDE1OCxcbiAgICA4NCw3LDk5LCAwLDgsMTI3LCAwLDgsNjMsIDAsOSwyMjIsXG4gICAgODIsNywyNywgMCw4LDExMSwgMCw4LDQ3LCAwLDksMTkwLFxuICAgIDAsOCwxNSwgMCw4LDE0MywgMCw4LDc5LCAwLDksMjU0LFxuICAgIDk2LDcsMjU2LCAwLDgsODAsIDAsOCwxNiwgODQsOCwxMTUsXG4gICAgODIsNywzMSwgMCw4LDExMiwgMCw4LDQ4LCAwLDksMTkzLFxuXG4gICAgODAsNywxMCwgMCw4LDk2LCAwLDgsMzIsIDAsOSwxNjEsXG4gICAgMCw4LDAsIDAsOCwxMjgsIDAsOCw2NCwgMCw5LDIyNSxcbiAgICA4MCw3LDYsIDAsOCw4OCwgMCw4LDI0LCAwLDksMTQ1LFxuICAgIDgzLDcsNTksIDAsOCwxMjAsIDAsOCw1NiwgMCw5LDIwOSxcbiAgICA4MSw3LDE3LCAwLDgsMTA0LCAwLDgsNDAsIDAsOSwxNzcsXG4gICAgMCw4LDgsIDAsOCwxMzYsIDAsOCw3MiwgMCw5LDI0MSxcbiAgICA4MCw3LDQsIDAsOCw4NCwgMCw4LDIwLCA4NSw4LDIyNyxcbiAgICA4Myw3LDQzLCAwLDgsMTE2LCAwLDgsNTIsIDAsOSwyMDEsXG4gICAgODEsNywxMywgMCw4LDEwMCwgMCw4LDM2LCAwLDksMTY5LFxuICAgIDAsOCw0LCAwLDgsMTMyLCAwLDgsNjgsIDAsOSwyMzMsXG4gICAgODAsNyw4LCAwLDgsOTIsIDAsOCwyOCwgMCw5LDE1MyxcbiAgICA4NCw3LDgzLCAwLDgsMTI0LCAwLDgsNjAsIDAsOSwyMTcsXG4gICAgODIsNywyMywgMCw4LDEwOCwgMCw4LDQ0LCAwLDksMTg1LFxuICAgIDAsOCwxMiwgMCw4LDE0MCwgMCw4LDc2LCAwLDksMjQ5LFxuICAgIDgwLDcsMywgMCw4LDgyLCAwLDgsMTgsIDg1LDgsMTYzLFxuICAgIDgzLDcsMzUsIDAsOCwxMTQsIDAsOCw1MCwgMCw5LDE5NyxcbiAgICA4MSw3LDExLCAwLDgsOTgsIDAsOCwzNCwgMCw5LDE2NSxcbiAgICAwLDgsMiwgMCw4LDEzMCwgMCw4LDY2LCAwLDksMjI5LFxuICAgIDgwLDcsNywgMCw4LDkwLCAwLDgsMjYsIDAsOSwxNDksXG4gICAgODQsNyw2NywgMCw4LDEyMiwgMCw4LDU4LCAwLDksMjEzLFxuICAgIDgyLDcsMTksIDAsOCwxMDYsIDAsOCw0MiwgMCw5LDE4MSxcbiAgICAwLDgsMTAsIDAsOCwxMzgsIDAsOCw3NCwgMCw5LDI0NSxcbiAgICA4MCw3LDUsIDAsOCw4NiwgMCw4LDIyLCAxOTIsOCwwLFxuICAgIDgzLDcsNTEsIDAsOCwxMTgsIDAsOCw1NCwgMCw5LDIwNSxcbiAgICA4MSw3LDE1LCAwLDgsMTAyLCAwLDgsMzgsIDAsOSwxNzMsXG4gICAgMCw4LDYsIDAsOCwxMzQsIDAsOCw3MCwgMCw5LDIzNyxcbiAgICA4MCw3LDksIDAsOCw5NCwgMCw4LDMwLCAwLDksMTU3LFxuICAgIDg0LDcsOTksIDAsOCwxMjYsIDAsOCw2MiwgMCw5LDIyMSxcbiAgICA4Miw3LDI3LCAwLDgsMTEwLCAwLDgsNDYsIDAsOSwxODksXG4gICAgMCw4LDE0LCAwLDgsMTQyLCAwLDgsNzgsIDAsOSwyNTMsXG4gICAgOTYsNywyNTYsIDAsOCw4MSwgMCw4LDE3LCA4NSw4LDEzMSxcbiAgICA4Miw3LDMxLCAwLDgsMTEzLCAwLDgsNDksIDAsOSwxOTUsXG4gICAgODAsNywxMCwgMCw4LDk3LCAwLDgsMzMsIDAsOSwxNjMsXG4gICAgMCw4LDEsIDAsOCwxMjksIDAsOCw2NSwgMCw5LDIyNyxcbiAgICA4MCw3LDYsIDAsOCw4OSwgMCw4LDI1LCAwLDksMTQ3LFxuICAgIDgzLDcsNTksIDAsOCwxMjEsIDAsOCw1NywgMCw5LDIxMSxcbiAgICA4MSw3LDE3LCAwLDgsMTA1LCAwLDgsNDEsIDAsOSwxNzksXG4gICAgMCw4LDksIDAsOCwxMzcsIDAsOCw3MywgMCw5LDI0MyxcbiAgICA4MCw3LDQsIDAsOCw4NSwgMCw4LDIxLCA4MCw4LDI1OCxcbiAgICA4Myw3LDQzLCAwLDgsMTE3LCAwLDgsNTMsIDAsOSwyMDMsXG4gICAgODEsNywxMywgMCw4LDEwMSwgMCw4LDM3LCAwLDksMTcxLFxuICAgIDAsOCw1LCAwLDgsMTMzLCAwLDgsNjksIDAsOSwyMzUsXG4gICAgODAsNyw4LCAwLDgsOTMsIDAsOCwyOSwgMCw5LDE1NSxcbiAgICA4NCw3LDgzLCAwLDgsMTI1LCAwLDgsNjEsIDAsOSwyMTksXG4gICAgODIsNywyMywgMCw4LDEwOSwgMCw4LDQ1LCAwLDksMTg3LFxuICAgIDAsOCwxMywgMCw4LDE0MSwgMCw4LDc3LCAwLDksMjUxLFxuICAgIDgwLDcsMywgMCw4LDgzLCAwLDgsMTksIDg1LDgsMTk1LFxuICAgIDgzLDcsMzUsIDAsOCwxMTUsIDAsOCw1MSwgMCw5LDE5OSxcbiAgICA4MSw3LDExLCAwLDgsOTksIDAsOCwzNSwgMCw5LDE2NyxcbiAgICAwLDgsMywgMCw4LDEzMSwgMCw4LDY3LCAwLDksMjMxLFxuICAgIDgwLDcsNywgMCw4LDkxLCAwLDgsMjcsIDAsOSwxNTEsXG4gICAgODQsNyw2NywgMCw4LDEyMywgMCw4LDU5LCAwLDksMjE1LFxuICAgIDgyLDcsMTksIDAsOCwxMDcsIDAsOCw0MywgMCw5LDE4MyxcbiAgICAwLDgsMTEsIDAsOCwxMzksIDAsOCw3NSwgMCw5LDI0NyxcbiAgICA4MCw3LDUsIDAsOCw4NywgMCw4LDIzLCAxOTIsOCwwLFxuICAgIDgzLDcsNTEsIDAsOCwxMTksIDAsOCw1NSwgMCw5LDIwNyxcbiAgICA4MSw3LDE1LCAwLDgsMTAzLCAwLDgsMzksIDAsOSwxNzUsXG4gICAgMCw4LDcsIDAsOCwxMzUsIDAsOCw3MSwgMCw5LDIzOSxcbiAgICA4MCw3LDksIDAsOCw5NSwgMCw4LDMxLCAwLDksMTU5LFxuICAgIDg0LDcsOTksIDAsOCwxMjcsIDAsOCw2MywgMCw5LDIyMyxcbiAgICA4Miw3LDI3LCAwLDgsMTExLCAwLDgsNDcsIDAsOSwxOTEsXG4gICAgMCw4LDE1LCAwLDgsMTQzLCAwLDgsNzksIDAsOSwyNTVcbl07XG52YXIgZml4ZWRfdGQgPSBbXG4gICAgODAsNSwxLCA4Nyw1LDI1NywgODMsNSwxNywgOTEsNSw0MDk3LFxuICAgIDgxLDUsNSwgODksNSwxMDI1LCA4NSw1LDY1LCA5Myw1LDE2Mzg1LFxuICAgIDgwLDUsMywgODgsNSw1MTMsIDg0LDUsMzMsIDkyLDUsODE5MyxcbiAgICA4Miw1LDksIDkwLDUsMjA0OSwgODYsNSwxMjksIDE5Miw1LDI0NTc3LFxuICAgIDgwLDUsMiwgODcsNSwzODUsIDgzLDUsMjUsIDkxLDUsNjE0NSxcbiAgICA4MSw1LDcsIDg5LDUsMTUzNywgODUsNSw5NywgOTMsNSwyNDU3NyxcbiAgICA4MCw1LDQsIDg4LDUsNzY5LCA4NCw1LDQ5LCA5Miw1LDEyMjg5LFxuICAgIDgyLDUsMTMsIDkwLDUsMzA3MywgODYsNSwxOTMsIDE5Miw1LDI0NTc3XG5dO1xuXG4gIC8vIFRhYmxlcyBmb3IgZGVmbGF0ZSBmcm9tIFBLWklQJ3MgYXBwbm90ZS50eHQuXG4gIHZhciBjcGxlbnMgPSBbIC8vIENvcHkgbGVuZ3RocyBmb3IgbGl0ZXJhbCBjb2RlcyAyNTcuLjI4NVxuICAgICAgICAzLCA0LCA1LCA2LCA3LCA4LCA5LCAxMCwgMTEsIDEzLCAxNSwgMTcsIDE5LCAyMywgMjcsIDMxLFxuICAgICAgICAzNSwgNDMsIDUxLCA1OSwgNjcsIDgzLCA5OSwgMTE1LCAxMzEsIDE2MywgMTk1LCAyMjcsIDI1OCwgMCwgMFxuICBdO1xuXG4gIC8vIHNlZSBub3RlICMxMyBhYm92ZSBhYm91dCAyNThcbiAgdmFyIGNwbGV4dCA9IFsgLy8gRXh0cmEgYml0cyBmb3IgbGl0ZXJhbCBjb2RlcyAyNTcuLjI4NVxuICAgICAgICAwLCAwLCAwLCAwLCAwLCAwLCAwLCAwLCAxLCAxLCAxLCAxLCAyLCAyLCAyLCAyLFxuICAgICAgICAzLCAzLCAzLCAzLCA0LCA0LCA0LCA0LCA1LCA1LCA1LCA1LCAwLCAxMTIsIDExMiAgLy8gMTEyPT1pbnZhbGlkXG4gIF07XG5cbiB2YXIgY3BkaXN0ID0gWyAvLyBDb3B5IG9mZnNldHMgZm9yIGRpc3RhbmNlIGNvZGVzIDAuLjI5XG4gICAgICAgIDEsIDIsIDMsIDQsIDUsIDcsIDksIDEzLCAxNywgMjUsIDMzLCA0OSwgNjUsIDk3LCAxMjksIDE5MyxcbiAgICAgICAgMjU3LCAzODUsIDUxMywgNzY5LCAxMDI1LCAxNTM3LCAyMDQ5LCAzMDczLCA0MDk3LCA2MTQ1LFxuICAgICAgICA4MTkzLCAxMjI4OSwgMTYzODUsIDI0NTc3XG4gIF07XG5cbiAgdmFyIGNwZGV4dCA9IFsgLy8gRXh0cmEgYml0cyBmb3IgZGlzdGFuY2UgY29kZXNcbiAgICAgICAgMCwgMCwgMCwgMCwgMSwgMSwgMiwgMiwgMywgMywgNCwgNCwgNSwgNSwgNiwgNixcbiAgICAgICAgNywgNywgOCwgOCwgOSwgOSwgMTAsIDEwLCAxMSwgMTEsXG4gICAgICAgIDEyLCAxMiwgMTMsIDEzXTtcblxuLy9cbi8vIFpTdHJlYW0uamF2YVxuLy9cblxuZnVuY3Rpb24gWlN0cmVhbSgpIHtcbn1cblxuXG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlSW5pdCA9IGZ1bmN0aW9uKHcsIG5vd3JhcCkge1xuICAgIGlmICghdykge1xuXHR3ID0gREVGX1dCSVRTO1xuICAgIH1cbiAgICBpZiAobm93cmFwKSB7XG5cdG5vd3JhcCA9IGZhbHNlO1xuICAgIH1cbiAgICB0aGlzLmlzdGF0ZSA9IG5ldyBJbmZsYXRlKCk7XG4gICAgcmV0dXJuIHRoaXMuaXN0YXRlLmluZmxhdGVJbml0KHRoaXMsIG5vd3JhcD8tdzp3KTtcbn1cblxuWlN0cmVhbS5wcm90b3R5cGUuaW5mbGF0ZSA9IGZ1bmN0aW9uKGYpIHtcbiAgICBpZih0aGlzLmlzdGF0ZT09bnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiB0aGlzLmlzdGF0ZS5pbmZsYXRlKHRoaXMsIGYpO1xufVxuXG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlRW5kID0gZnVuY3Rpb24oKXtcbiAgICBpZih0aGlzLmlzdGF0ZT09bnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHZhciByZXQ9aXN0YXRlLmluZmxhdGVFbmQodGhpcyk7XG4gICAgdGhpcy5pc3RhdGUgPSBudWxsO1xuICAgIHJldHVybiByZXQ7XG59XG5aU3RyZWFtLnByb3RvdHlwZS5pbmZsYXRlU3luYyA9IGZ1bmN0aW9uKCl7XG4gICAgLy8gaWYoaXN0YXRlID09IG51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gaXN0YXRlLmluZmxhdGVTeW5jKHRoaXMpO1xufVxuWlN0cmVhbS5wcm90b3R5cGUuaW5mbGF0ZVNldERpY3Rpb25hcnkgPSBmdW5jdGlvbihkaWN0aW9uYXJ5LCBkaWN0TGVuZ3RoKXtcbiAgICAvLyBpZihpc3RhdGUgPT0gbnVsbCkgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiBpc3RhdGUuaW5mbGF0ZVNldERpY3Rpb25hcnkodGhpcywgZGljdGlvbmFyeSwgZGljdExlbmd0aCk7XG59XG5cbi8qXG5cbiAgcHVibGljIGludCBkZWZsYXRlSW5pdChpbnQgbGV2ZWwpe1xuICAgIHJldHVybiBkZWZsYXRlSW5pdChsZXZlbCwgTUFYX1dCSVRTKTtcbiAgfVxuICBwdWJsaWMgaW50IGRlZmxhdGVJbml0KGludCBsZXZlbCwgYm9vbGVhbiBub3dyYXApe1xuICAgIHJldHVybiBkZWZsYXRlSW5pdChsZXZlbCwgTUFYX1dCSVRTLCBub3dyYXApO1xuICB9XG4gIHB1YmxpYyBpbnQgZGVmbGF0ZUluaXQoaW50IGxldmVsLCBpbnQgYml0cyl7XG4gICAgcmV0dXJuIGRlZmxhdGVJbml0KGxldmVsLCBiaXRzLCBmYWxzZSk7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlSW5pdChpbnQgbGV2ZWwsIGludCBiaXRzLCBib29sZWFuIG5vd3JhcCl7XG4gICAgZHN0YXRlPW5ldyBEZWZsYXRlKCk7XG4gICAgcmV0dXJuIGRzdGF0ZS5kZWZsYXRlSW5pdCh0aGlzLCBsZXZlbCwgbm93cmFwPy1iaXRzOmJpdHMpO1xuICB9XG4gIHB1YmxpYyBpbnQgZGVmbGF0ZShpbnQgZmx1c2gpe1xuICAgIGlmKGRzdGF0ZT09bnVsbCl7XG4gICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgfVxuICAgIHJldHVybiBkc3RhdGUuZGVmbGF0ZSh0aGlzLCBmbHVzaCk7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlRW5kKCl7XG4gICAgaWYoZHN0YXRlPT1udWxsKSByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgaW50IHJldD1kc3RhdGUuZGVmbGF0ZUVuZCgpO1xuICAgIGRzdGF0ZT1udWxsO1xuICAgIHJldHVybiByZXQ7XG4gIH1cbiAgcHVibGljIGludCBkZWZsYXRlUGFyYW1zKGludCBsZXZlbCwgaW50IHN0cmF0ZWd5KXtcbiAgICBpZihkc3RhdGU9PW51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gZHN0YXRlLmRlZmxhdGVQYXJhbXModGhpcywgbGV2ZWwsIHN0cmF0ZWd5KTtcbiAgfVxuICBwdWJsaWMgaW50IGRlZmxhdGVTZXREaWN0aW9uYXJ5IChieXRlW10gZGljdGlvbmFyeSwgaW50IGRpY3RMZW5ndGgpe1xuICAgIGlmKGRzdGF0ZSA9PSBudWxsKVxuICAgICAgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIHJldHVybiBkc3RhdGUuZGVmbGF0ZVNldERpY3Rpb25hcnkodGhpcywgZGljdGlvbmFyeSwgZGljdExlbmd0aCk7XG4gIH1cblxuKi9cblxuLypcbiAgLy8gRmx1c2ggYXMgbXVjaCBwZW5kaW5nIG91dHB1dCBhcyBwb3NzaWJsZS4gQWxsIGRlZmxhdGUoKSBvdXRwdXQgZ29lc1xuICAvLyB0aHJvdWdoIHRoaXMgZnVuY3Rpb24gc28gc29tZSBhcHBsaWNhdGlvbnMgbWF5IHdpc2ggdG8gbW9kaWZ5IGl0XG4gIC8vIHRvIGF2b2lkIGFsbG9jYXRpbmcgYSBsYXJnZSBzdHJtLT5uZXh0X291dCBidWZmZXIgYW5kIGNvcHlpbmcgaW50byBpdC5cbiAgLy8gKFNlZSBhbHNvIHJlYWRfYnVmKCkpLlxuICB2b2lkIGZsdXNoX3BlbmRpbmcoKXtcbiAgICBpbnQgbGVuPWRzdGF0ZS5wZW5kaW5nO1xuXG4gICAgaWYobGVuPmF2YWlsX291dCkgbGVuPWF2YWlsX291dDtcbiAgICBpZihsZW49PTApIHJldHVybjtcblxuICAgIGlmKGRzdGF0ZS5wZW5kaW5nX2J1Zi5sZW5ndGg8PWRzdGF0ZS5wZW5kaW5nX291dCB8fFxuICAgICAgIG5leHRfb3V0Lmxlbmd0aDw9bmV4dF9vdXRfaW5kZXggfHxcbiAgICAgICBkc3RhdGUucGVuZGluZ19idWYubGVuZ3RoPChkc3RhdGUucGVuZGluZ19vdXQrbGVuKSB8fFxuICAgICAgIG5leHRfb3V0Lmxlbmd0aDwobmV4dF9vdXRfaW5kZXgrbGVuKSl7XG4gICAgICBTeXN0ZW0ub3V0LnByaW50bG4oZHN0YXRlLnBlbmRpbmdfYnVmLmxlbmd0aCtcIiwgXCIrZHN0YXRlLnBlbmRpbmdfb3V0K1xuXHRcdFx0IFwiLCBcIituZXh0X291dC5sZW5ndGgrXCIsIFwiK25leHRfb3V0X2luZGV4K1wiLCBcIitsZW4pO1xuICAgICAgU3lzdGVtLm91dC5wcmludGxuKFwiYXZhaWxfb3V0PVwiK2F2YWlsX291dCk7XG4gICAgfVxuXG4gICAgU3lzdGVtLmFycmF5Y29weShkc3RhdGUucGVuZGluZ19idWYsIGRzdGF0ZS5wZW5kaW5nX291dCxcblx0XHQgICAgIG5leHRfb3V0LCBuZXh0X291dF9pbmRleCwgbGVuKTtcblxuICAgIG5leHRfb3V0X2luZGV4Kz1sZW47XG4gICAgZHN0YXRlLnBlbmRpbmdfb3V0Kz1sZW47XG4gICAgdG90YWxfb3V0Kz1sZW47XG4gICAgYXZhaWxfb3V0LT1sZW47XG4gICAgZHN0YXRlLnBlbmRpbmctPWxlbjtcbiAgICBpZihkc3RhdGUucGVuZGluZz09MCl7XG4gICAgICBkc3RhdGUucGVuZGluZ19vdXQ9MDtcbiAgICB9XG4gIH1cblxuICAvLyBSZWFkIGEgbmV3IGJ1ZmZlciBmcm9tIHRoZSBjdXJyZW50IGlucHV0IHN0cmVhbSwgdXBkYXRlIHRoZSBhZGxlcjMyXG4gIC8vIGFuZCB0b3RhbCBudW1iZXIgb2YgYnl0ZXMgcmVhZC4gIEFsbCBkZWZsYXRlKCkgaW5wdXQgZ29lcyB0aHJvdWdoXG4gIC8vIHRoaXMgZnVuY3Rpb24gc28gc29tZSBhcHBsaWNhdGlvbnMgbWF5IHdpc2ggdG8gbW9kaWZ5IGl0IHRvIGF2b2lkXG4gIC8vIGFsbG9jYXRpbmcgYSBsYXJnZSBzdHJtLT5uZXh0X2luIGJ1ZmZlciBhbmQgY29weWluZyBmcm9tIGl0LlxuICAvLyAoU2VlIGFsc28gZmx1c2hfcGVuZGluZygpKS5cbiAgaW50IHJlYWRfYnVmKGJ5dGVbXSBidWYsIGludCBzdGFydCwgaW50IHNpemUpIHtcbiAgICBpbnQgbGVuPWF2YWlsX2luO1xuXG4gICAgaWYobGVuPnNpemUpIGxlbj1zaXplO1xuICAgIGlmKGxlbj09MCkgcmV0dXJuIDA7XG5cbiAgICBhdmFpbF9pbi09bGVuO1xuXG4gICAgaWYoZHN0YXRlLm5vaGVhZGVyPT0wKSB7XG4gICAgICBhZGxlcj1fYWRsZXIuYWRsZXIzMihhZGxlciwgbmV4dF9pbiwgbmV4dF9pbl9pbmRleCwgbGVuKTtcbiAgICB9XG4gICAgU3lzdGVtLmFycmF5Y29weShuZXh0X2luLCBuZXh0X2luX2luZGV4LCBidWYsIHN0YXJ0LCBsZW4pO1xuICAgIG5leHRfaW5faW5kZXggICs9IGxlbjtcbiAgICB0b3RhbF9pbiArPSBsZW47XG4gICAgcmV0dXJuIGxlbjtcbiAgfVxuXG4gIHB1YmxpYyB2b2lkIGZyZWUoKXtcbiAgICBuZXh0X2luPW51bGw7XG4gICAgbmV4dF9vdXQ9bnVsbDtcbiAgICBtc2c9bnVsbDtcbiAgICBfYWRsZXI9bnVsbDtcbiAgfVxufVxuKi9cblxuXG4vL1xuLy8gSW5mbGF0ZS5qYXZhXG4vL1xuXG5mdW5jdGlvbiBJbmZsYXRlKCkge1xuICAgIHRoaXMud2FzID0gWzBdO1xufVxuXG5JbmZsYXRlLnByb3RvdHlwZS5pbmZsYXRlUmVzZXQgPSBmdW5jdGlvbih6KSB7XG4gICAgaWYoeiA9PSBudWxsIHx8IHouaXN0YXRlID09IG51bGwpIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICBcbiAgICB6LnRvdGFsX2luID0gei50b3RhbF9vdXQgPSAwO1xuICAgIHoubXNnID0gbnVsbDtcbiAgICB6LmlzdGF0ZS5tb2RlID0gei5pc3RhdGUubm93cmFwIT0wID8gQkxPQ0tTIDogTUVUSE9EO1xuICAgIHouaXN0YXRlLmJsb2Nrcy5yZXNldCh6LCBudWxsKTtcbiAgICByZXR1cm4gWl9PSztcbn1cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZUVuZCA9IGZ1bmN0aW9uKHope1xuICAgIGlmKHRoaXMuYmxvY2tzICE9IG51bGwpXG4gICAgICB0aGlzLmJsb2Nrcy5mcmVlKHopO1xuICAgIHRoaXMuYmxvY2tzPW51bGw7XG4gICAgcmV0dXJuIFpfT0s7XG59XG5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGVJbml0ID0gZnVuY3Rpb24oeiwgdyl7XG4gICAgei5tc2cgPSBudWxsO1xuICAgIHRoaXMuYmxvY2tzID0gbnVsbDtcblxuICAgIC8vIGhhbmRsZSB1bmRvY3VtZW50ZWQgbm93cmFwIG9wdGlvbiAobm8gemxpYiBoZWFkZXIgb3IgY2hlY2spXG4gICAgbm93cmFwID0gMDtcbiAgICBpZih3IDwgMCl7XG4gICAgICB3ID0gLSB3O1xuICAgICAgbm93cmFwID0gMTtcbiAgICB9XG5cbiAgICAvLyBzZXQgd2luZG93IHNpemVcbiAgICBpZih3PDggfHx3PjE1KXtcbiAgICAgIHRoaXMuaW5mbGF0ZUVuZCh6KTtcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICB9XG4gICAgdGhpcy53Yml0cz13O1xuXG4gICAgei5pc3RhdGUuYmxvY2tzPW5ldyBJbmZCbG9ja3MoeiwgXG5cdFx0XHRcdCAgei5pc3RhdGUubm93cmFwIT0wID8gbnVsbCA6IHRoaXMsXG5cdFx0XHRcdCAgMTw8dyk7XG5cbiAgICAvLyByZXNldCBzdGF0ZVxuICAgIHRoaXMuaW5mbGF0ZVJlc2V0KHopO1xuICAgIHJldHVybiBaX09LO1xuICB9XG5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGUgPSBmdW5jdGlvbih6LCBmKXtcbiAgICB2YXIgciwgYjtcblxuICAgIGlmKHogPT0gbnVsbCB8fCB6LmlzdGF0ZSA9PSBudWxsIHx8IHoubmV4dF9pbiA9PSBudWxsKVxuICAgICAgcmV0dXJuIFpfU1RSRUFNX0VSUk9SO1xuICAgIGYgPSBmID09IFpfRklOSVNIID8gWl9CVUZfRVJST1IgOiBaX09LO1xuICAgIHIgPSBaX0JVRl9FUlJPUjtcbiAgICB3aGlsZSAodHJ1ZSl7XG4gICAgICBzd2l0Y2ggKHouaXN0YXRlLm1vZGUpe1xuICAgICAgY2FzZSBNRVRIT0Q6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIGlmKCgoei5pc3RhdGUubWV0aG9kID0gei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSkmMHhmKSE9Wl9ERUZMQVRFRCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZz1cInVua25vd24gY29tcHJlc3Npb24gbWV0aG9kXCI7XG4gICAgICAgICAgei5pc3RhdGUubWFya2VyID0gNTsgICAgICAgLy8gY2FuJ3QgdHJ5IGluZmxhdGVTeW5jXG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIH1cbiAgICAgICAgaWYoKHouaXN0YXRlLm1ldGhvZD4+NCkrOD56LmlzdGF0ZS53Yml0cyl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZz1cImludmFsaWQgd2luZG93IHNpemVcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICB6LmlzdGF0ZS5tb2RlPUZMQUc7XG4gICAgICBjYXNlIEZMQUc6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIGIgPSAoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSkmMHhmZjtcblxuICAgICAgICBpZigoKCh6LmlzdGF0ZS5tZXRob2QgPDwgOCkrYikgJSAzMSkhPTApe1xuICAgICAgICAgIHouaXN0YXRlLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5tc2cgPSBcImluY29ycmVjdCBoZWFkZXIgY2hlY2tcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuXG4gICAgICAgIGlmKChiJlBSRVNFVF9ESUNUKT09MCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJMT0NLUztcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICB6LmlzdGF0ZS5tb2RlID0gRElDVDQ7XG4gICAgICBjYXNlIERJQ1Q0OlxuXG4gICAgICAgIGlmKHouYXZhaWxfaW49PTApcmV0dXJuIHI7cj1mO1xuXG4gICAgICAgIHouYXZhaWxfaW4tLTsgei50b3RhbF9pbisrO1xuICAgICAgICB6LmlzdGF0ZS5uZWVkPSgoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTw8MjQpJjB4ZmYwMDAwMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGU9RElDVDM7XG4gICAgICBjYXNlIERJQ1QzOlxuXG4gICAgICAgIGlmKHouYXZhaWxfaW49PTApcmV0dXJuIHI7cj1mO1xuXG4gICAgICAgIHouYXZhaWxfaW4tLTsgei50b3RhbF9pbisrO1xuICAgICAgICB6LmlzdGF0ZS5uZWVkKz0oKHoubmV4dF9pblt6Lm5leHRfaW5faW5kZXgrK10mMHhmZik8PDE2KSYweGZmMDAwMDtcbiAgICAgICAgei5pc3RhdGUubW9kZT1ESUNUMjtcbiAgICAgIGNhc2UgRElDVDI6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIHouaXN0YXRlLm5lZWQrPSgoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTw8OCkmMHhmZjAwO1xuICAgICAgICB6LmlzdGF0ZS5tb2RlPURJQ1QxO1xuICAgICAgY2FzZSBESUNUMTpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCArPSAoei5uZXh0X2luW3oubmV4dF9pbl9pbmRleCsrXSYweGZmKTtcbiAgICAgICAgei5hZGxlciA9IHouaXN0YXRlLm5lZWQ7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBESUNUMDtcbiAgICAgICAgcmV0dXJuIFpfTkVFRF9ESUNUO1xuICAgICAgY2FzZSBESUNUMDpcbiAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgei5tc2cgPSBcIm5lZWQgZGljdGlvbmFyeVwiO1xuICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSAwOyAgICAgICAvLyBjYW4gdHJ5IGluZmxhdGVTeW5jXG4gICAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICAgIGNhc2UgQkxPQ0tTOlxuXG4gICAgICAgIHIgPSB6LmlzdGF0ZS5ibG9ja3MucHJvYyh6LCByKTtcbiAgICAgICAgaWYociA9PSBaX0RBVEFfRVJST1Ipe1xuICAgICAgICAgIHouaXN0YXRlLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5pc3RhdGUubWFya2VyID0gMDsgICAgIC8vIGNhbiB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuICAgICAgICBpZihyID09IFpfT0spe1xuICAgICAgICAgIHIgPSBmO1xuICAgICAgICB9XG4gICAgICAgIGlmKHIgIT0gWl9TVFJFQU1fRU5EKXtcbiAgICAgICAgICByZXR1cm4gcjtcbiAgICAgICAgfVxuICAgICAgICByID0gZjtcbiAgICAgICAgei5pc3RhdGUuYmxvY2tzLnJlc2V0KHosIHouaXN0YXRlLndhcyk7XG4gICAgICAgIGlmKHouaXN0YXRlLm5vd3JhcCE9MCl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZT1ET05FO1xuICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgICAgIHouaXN0YXRlLm1vZGU9Q0hFQ0s0O1xuICAgICAgY2FzZSBDSEVDSzQ6XG5cbiAgICAgICAgaWYoei5hdmFpbF9pbj09MClyZXR1cm4gcjtyPWY7XG5cbiAgICAgICAgei5hdmFpbF9pbi0tOyB6LnRvdGFsX2luKys7XG4gICAgICAgIHouaXN0YXRlLm5lZWQ9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDwyNCkmMHhmZjAwMDAwMDtcbiAgICAgICAgei5pc3RhdGUubW9kZT1DSEVDSzM7XG4gICAgICBjYXNlIENIRUNLMzpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDwxNikmMHhmZjAwMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBDSEVDSzI7XG4gICAgICBjYXNlIENIRUNLMjpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KCh6Lm5leHRfaW5bei5uZXh0X2luX2luZGV4KytdJjB4ZmYpPDw4KSYweGZmMDA7XG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBDSEVDSzE7XG4gICAgICBjYXNlIENIRUNLMTpcblxuICAgICAgICBpZih6LmF2YWlsX2luPT0wKXJldHVybiByO3I9ZjtcblxuICAgICAgICB6LmF2YWlsX2luLS07IHoudG90YWxfaW4rKztcbiAgICAgICAgei5pc3RhdGUubmVlZCs9KHoubmV4dF9pblt6Lm5leHRfaW5faW5kZXgrK10mMHhmZik7XG5cbiAgICAgICAgaWYoKCh6LmlzdGF0ZS53YXNbMF0pKSAhPSAoKHouaXN0YXRlLm5lZWQpKSl7XG4gICAgICAgICAgei5pc3RhdGUubW9kZSA9IEJBRDtcbiAgICAgICAgICB6Lm1zZyA9IFwiaW5jb3JyZWN0IGRhdGEgY2hlY2tcIjtcbiAgICAgICAgICB6LmlzdGF0ZS5tYXJrZXIgPSA1OyAgICAgICAvLyBjYW4ndCB0cnkgaW5mbGF0ZVN5bmNcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgfVxuXG4gICAgICAgIHouaXN0YXRlLm1vZGUgPSBET05FO1xuICAgICAgY2FzZSBET05FOlxuICAgICAgICByZXR1cm4gWl9TVFJFQU1fRU5EO1xuICAgICAgY2FzZSBCQUQ6XG4gICAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgICBkZWZhdWx0OlxuICAgICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG4gICAgICB9XG4gICAgfVxuICB9XG5cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZVNldERpY3Rpb25hcnkgPSBmdW5jdGlvbih6LCAgZGljdGlvbmFyeSwgZGljdExlbmd0aCkge1xuICAgIHZhciBpbmRleD0wO1xuICAgIHZhciBsZW5ndGggPSBkaWN0TGVuZ3RoO1xuICAgIGlmKHo9PW51bGwgfHwgei5pc3RhdGUgPT0gbnVsbHx8IHouaXN0YXRlLm1vZGUgIT0gRElDVDApXG4gICAgICByZXR1cm4gWl9TVFJFQU1fRVJST1I7XG5cbiAgICBpZih6Ll9hZGxlci5hZGxlcjMyKDEsIGRpY3Rpb25hcnksIDAsIGRpY3RMZW5ndGgpIT16LmFkbGVyKXtcbiAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgfVxuXG4gICAgei5hZGxlciA9IHouX2FkbGVyLmFkbGVyMzIoMCwgbnVsbCwgMCwgMCk7XG5cbiAgICBpZihsZW5ndGggPj0gKDE8PHouaXN0YXRlLndiaXRzKSl7XG4gICAgICBsZW5ndGggPSAoMTw8ei5pc3RhdGUud2JpdHMpLTE7XG4gICAgICBpbmRleD1kaWN0TGVuZ3RoIC0gbGVuZ3RoO1xuICAgIH1cbiAgICB6LmlzdGF0ZS5ibG9ja3Muc2V0X2RpY3Rpb25hcnkoZGljdGlvbmFyeSwgaW5kZXgsIGxlbmd0aCk7XG4gICAgei5pc3RhdGUubW9kZSA9IEJMT0NLUztcbiAgICByZXR1cm4gWl9PSztcbiAgfVxuXG4vLyAgc3RhdGljIHByaXZhdGUgYnl0ZVtdIG1hcmsgPSB7KGJ5dGUpMCwgKGJ5dGUpMCwgKGJ5dGUpMHhmZiwgKGJ5dGUpMHhmZn07XG52YXIgbWFyayA9IFswLCAwLCAyNTUsIDI1NV1cblxuSW5mbGF0ZS5wcm90b3R5cGUuaW5mbGF0ZVN5bmMgPSBmdW5jdGlvbih6KXtcbiAgICB2YXIgbjsgICAgICAgLy8gbnVtYmVyIG9mIGJ5dGVzIHRvIGxvb2sgYXRcbiAgICB2YXIgcDsgICAgICAgLy8gcG9pbnRlciB0byBieXRlc1xuICAgIHZhciBtOyAgICAgICAvLyBudW1iZXIgb2YgbWFya2VyIGJ5dGVzIGZvdW5kIGluIGEgcm93XG4gICAgdmFyIHIsIHc7ICAgLy8gdGVtcG9yYXJpZXMgdG8gc2F2ZSB0b3RhbF9pbiBhbmQgdG90YWxfb3V0XG5cbiAgICAvLyBzZXQgdXBcbiAgICBpZih6ID09IG51bGwgfHwgei5pc3RhdGUgPT0gbnVsbClcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICBpZih6LmlzdGF0ZS5tb2RlICE9IEJBRCl7XG4gICAgICB6LmlzdGF0ZS5tb2RlID0gQkFEO1xuICAgICAgei5pc3RhdGUubWFya2VyID0gMDtcbiAgICB9XG4gICAgaWYoKG49ei5hdmFpbF9pbik9PTApXG4gICAgICByZXR1cm4gWl9CVUZfRVJST1I7XG4gICAgcD16Lm5leHRfaW5faW5kZXg7XG4gICAgbT16LmlzdGF0ZS5tYXJrZXI7XG5cbiAgICAvLyBzZWFyY2hcbiAgICB3aGlsZSAobiE9MCAmJiBtIDwgNCl7XG4gICAgICBpZih6Lm5leHRfaW5bcF0gPT0gbWFya1ttXSl7XG4gICAgICAgIG0rKztcbiAgICAgIH1cbiAgICAgIGVsc2UgaWYoei5uZXh0X2luW3BdIT0wKXtcbiAgICAgICAgbSA9IDA7XG4gICAgICB9XG4gICAgICBlbHNle1xuICAgICAgICBtID0gNCAtIG07XG4gICAgICB9XG4gICAgICBwKys7IG4tLTtcbiAgICB9XG5cbiAgICAvLyByZXN0b3JlXG4gICAgei50b3RhbF9pbiArPSBwLXoubmV4dF9pbl9pbmRleDtcbiAgICB6Lm5leHRfaW5faW5kZXggPSBwO1xuICAgIHouYXZhaWxfaW4gPSBuO1xuICAgIHouaXN0YXRlLm1hcmtlciA9IG07XG5cbiAgICAvLyByZXR1cm4gbm8gam95IG9yIHNldCB1cCB0byByZXN0YXJ0IG9uIGEgbmV3IGJsb2NrXG4gICAgaWYobSAhPSA0KXtcbiAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgfVxuICAgIHI9ei50b3RhbF9pbjsgIHc9ei50b3RhbF9vdXQ7XG4gICAgdGhpcy5pbmZsYXRlUmVzZXQoeik7XG4gICAgei50b3RhbF9pbj1yOyAgei50b3RhbF9vdXQgPSB3O1xuICAgIHouaXN0YXRlLm1vZGUgPSBCTE9DS1M7XG4gICAgcmV0dXJuIFpfT0s7XG59XG5cbiAgLy8gUmV0dXJucyB0cnVlIGlmIGluZmxhdGUgaXMgY3VycmVudGx5IGF0IHRoZSBlbmQgb2YgYSBibG9jayBnZW5lcmF0ZWRcbiAgLy8gYnkgWl9TWU5DX0ZMVVNIIG9yIFpfRlVMTF9GTFVTSC4gVGhpcyBmdW5jdGlvbiBpcyB1c2VkIGJ5IG9uZSBQUFBcbiAgLy8gaW1wbGVtZW50YXRpb24gdG8gcHJvdmlkZSBhbiBhZGRpdGlvbmFsIHNhZmV0eSBjaGVjay4gUFBQIHVzZXMgWl9TWU5DX0ZMVVNIXG4gIC8vIGJ1dCByZW1vdmVzIHRoZSBsZW5ndGggYnl0ZXMgb2YgdGhlIHJlc3VsdGluZyBlbXB0eSBzdG9yZWQgYmxvY2suIFdoZW5cbiAgLy8gZGVjb21wcmVzc2luZywgUFBQIGNoZWNrcyB0aGF0IGF0IHRoZSBlbmQgb2YgaW5wdXQgcGFja2V0LCBpbmZsYXRlIGlzXG4gIC8vIHdhaXRpbmcgZm9yIHRoZXNlIGxlbmd0aCBieXRlcy5cbkluZmxhdGUucHJvdG90eXBlLmluZmxhdGVTeW5jUG9pbnQgPSBmdW5jdGlvbih6KXtcbiAgICBpZih6ID09IG51bGwgfHwgei5pc3RhdGUgPT0gbnVsbCB8fCB6LmlzdGF0ZS5ibG9ja3MgPT0gbnVsbClcbiAgICAgIHJldHVybiBaX1NUUkVBTV9FUlJPUjtcbiAgICByZXR1cm4gei5pc3RhdGUuYmxvY2tzLnN5bmNfcG9pbnQoKTtcbn1cblxuXG4vL1xuLy8gSW5mQmxvY2tzLmphdmFcbi8vXG5cbnZhciBJTkZCTE9DS1NfQk9SREVSID0gWzE2LCAxNywgMTgsIDAsIDgsIDcsIDksIDYsIDEwLCA1LCAxMSwgNCwgMTIsIDMsIDEzLCAyLCAxNCwgMSwgMTVdO1xuXG5mdW5jdGlvbiBJbmZCbG9ja3MoeiwgY2hlY2tmbiwgdykge1xuICAgIHRoaXMuaHVmdHM9bmV3IEludDMyQXJyYXkoTUFOWSozKTtcbiAgICB0aGlzLndpbmRvdz1uZXcgVWludDhBcnJheSh3KTtcbiAgICB0aGlzLmVuZD13O1xuICAgIHRoaXMuY2hlY2tmbiA9IGNoZWNrZm47XG4gICAgdGhpcy5tb2RlID0gSUJfVFlQRTtcbiAgICB0aGlzLnJlc2V0KHosIG51bGwpO1xuXG4gICAgdGhpcy5sZWZ0ID0gMDsgICAgICAgICAgICAvLyBpZiBTVE9SRUQsIGJ5dGVzIGxlZnQgdG8gY29weSBcblxuICAgIHRoaXMudGFibGUgPSAwOyAgICAgICAgICAgLy8gdGFibGUgbGVuZ3RocyAoMTQgYml0cykgXG4gICAgdGhpcy5pbmRleCA9IDA7ICAgICAgICAgICAvLyBpbmRleCBpbnRvIGJsZW5zIChvciBib3JkZXIpIFxuICAgIHRoaXMuYmxlbnMgPSBudWxsOyAgICAgICAgIC8vIGJpdCBsZW5ndGhzIG9mIGNvZGVzIFxuICAgIHRoaXMuYmI9bmV3IEludDMyQXJyYXkoMSk7IC8vIGJpdCBsZW5ndGggdHJlZSBkZXB0aCBcbiAgICB0aGlzLnRiPW5ldyBJbnQzMkFycmF5KDEpOyAvLyBiaXQgbGVuZ3RoIGRlY29kaW5nIHRyZWUgXG5cbiAgICB0aGlzLmNvZGVzID0gbmV3IEluZkNvZGVzKCk7XG5cbiAgICB0aGlzLmxhc3QgPSAwOyAgICAgICAgICAgIC8vIHRydWUgaWYgdGhpcyBibG9jayBpcyB0aGUgbGFzdCBibG9jayBcblxuICAvLyBtb2RlIGluZGVwZW5kZW50IGluZm9ybWF0aW9uIFxuICAgIHRoaXMuYml0ayA9IDA7ICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyIFxuICAgIHRoaXMuYml0YiA9IDA7ICAgICAgICAgICAgLy8gYml0IGJ1ZmZlciBcbiAgICB0aGlzLnJlYWQgPSAwOyAgICAgICAgICAgIC8vIHdpbmRvdyByZWFkIHBvaW50ZXIgXG4gICAgdGhpcy53cml0ZSA9IDA7ICAgICAgICAgICAvLyB3aW5kb3cgd3JpdGUgcG9pbnRlciBcbiAgICB0aGlzLmNoZWNrID0gMDsgICAgICAgICAgLy8gY2hlY2sgb24gb3V0cHV0IFxuXG4gICAgdGhpcy5pbmZ0cmVlPW5ldyBJbmZUcmVlKCk7XG59XG5cblxuXG5cbkluZkJsb2Nrcy5wcm90b3R5cGUucmVzZXQgPSBmdW5jdGlvbih6LCBjKXtcbiAgICBpZihjKSBjWzBdPXRoaXMuY2hlY2s7XG4gICAgaWYodGhpcy5tb2RlPT1JQl9DT0RFUyl7XG4gICAgICB0aGlzLmNvZGVzLmZyZWUoeik7XG4gICAgfVxuICAgIHRoaXMubW9kZT1JQl9UWVBFO1xuICAgIHRoaXMuYml0az0wO1xuICAgIHRoaXMuYml0Yj0wO1xuICAgIHRoaXMucmVhZD10aGlzLndyaXRlPTA7XG5cbiAgICBpZih0aGlzLmNoZWNrZm4pXG4gICAgICB6LmFkbGVyPXRoaXMuY2hlY2s9ei5fYWRsZXIuYWRsZXIzMigwLCBudWxsLCAwLCAwKTtcbiAgfVxuXG4gSW5mQmxvY2tzLnByb3RvdHlwZS5wcm9jID0gZnVuY3Rpb24oeiwgcil7XG4gICAgdmFyIHQ7ICAgICAgICAgICAgICAvLyB0ZW1wb3Jhcnkgc3RvcmFnZVxuICAgIHZhciBiOyAgICAgICAgICAgICAgLy8gYml0IGJ1ZmZlclxuICAgIHZhciBrOyAgICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyXG4gICAgdmFyIHA7ICAgICAgICAgICAgICAvLyBpbnB1dCBkYXRhIHBvaW50ZXJcbiAgICB2YXIgbjsgICAgICAgICAgICAgIC8vIGJ5dGVzIGF2YWlsYWJsZSB0aGVyZVxuICAgIHZhciBxOyAgICAgICAgICAgICAgLy8gb3V0cHV0IHdpbmRvdyB3cml0ZSBwb2ludGVyXG4gICAgdmFyIG07ICAgICAgICAgICAgICAvLyBieXRlcyB0byBlbmQgb2Ygd2luZG93IG9yIHJlYWQgcG9pbnRlclxuXG4gICAgLy8gY29weSBpbnB1dC9vdXRwdXQgaW5mb3JtYXRpb24gdG8gbG9jYWxzIChVUERBVEUgbWFjcm8gcmVzdG9yZXMpXG4gICAge3A9ei5uZXh0X2luX2luZGV4O249ei5hdmFpbF9pbjtiPXRoaXMuYml0YjtrPXRoaXMuYml0azt9XG4gICAge3E9dGhpcy53cml0ZTttPShxPHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTt9XG5cbiAgICAvLyBwcm9jZXNzIGlucHV0IGJhc2VkIG9uIGN1cnJlbnQgc3RhdGVcbiAgICB3aGlsZSh0cnVlKXtcbiAgICAgIHN3aXRjaCAodGhpcy5tb2RlKXtcbiAgICAgIGNhc2UgSUJfVFlQRTpcblxuXHR3aGlsZShrPCgzKSl7XG5cdCAgaWYobiE9MCl7XG5cdCAgICByPVpfT0s7XG5cdCAgfVxuXHQgIGVsc2V7XG5cdCAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgei5hdmFpbF9pbj1uO1xuXHQgICAgei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICB0aGlzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfTtcblx0ICBuLS07XG5cdCAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgIGsrPTg7XG5cdH1cblx0dCA9IChiICYgNyk7XG5cdHRoaXMubGFzdCA9IHQgJiAxO1xuXG5cdHN3aXRjaCAodCA+Pj4gMSl7XG4gICAgICAgIGNhc2UgMDogICAgICAgICAgICAgICAgICAgICAgICAgLy8gc3RvcmVkIFxuICAgICAgICAgIHtiPj4+PSgzKTtrLT0oMyk7fVxuICAgICAgICAgIHQgPSBrICYgNzsgICAgICAgICAgICAgICAgICAgIC8vIGdvIHRvIGJ5dGUgYm91bmRhcnlcblxuICAgICAgICAgIHtiPj4+PSh0KTtrLT0odCk7fVxuICAgICAgICAgIHRoaXMubW9kZSA9IElCX0xFTlM7ICAgICAgICAgICAgICAgICAgLy8gZ2V0IGxlbmd0aCBvZiBzdG9yZWQgYmxvY2tcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAxOiAgICAgICAgICAgICAgICAgICAgICAgICAvLyBmaXhlZFxuICAgICAgICAgIHtcbiAgICAgICAgICAgICAgdmFyIGJsPW5ldyBJbnQzMkFycmF5KDEpO1xuXHQgICAgICB2YXIgYmQ9bmV3IEludDMyQXJyYXkoMSk7XG4gICAgICAgICAgICAgIHZhciB0bD1bXTtcblx0ICAgICAgdmFyIHRkPVtdO1xuXG5cdCAgICAgIGluZmxhdGVfdHJlZXNfZml4ZWQoYmwsIGJkLCB0bCwgdGQsIHopO1xuICAgICAgICAgICAgICB0aGlzLmNvZGVzLmluaXQoYmxbMF0sIGJkWzBdLCB0bFswXSwgMCwgdGRbMF0sIDAsIHopO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIHtiPj4+PSgzKTtrLT0oMyk7fVxuXG4gICAgICAgICAgdGhpcy5tb2RlID0gSUJfQ09ERVM7XG4gICAgICAgICAgYnJlYWs7XG4gICAgICAgIGNhc2UgMjogICAgICAgICAgICAgICAgICAgICAgICAgLy8gZHluYW1pY1xuXG4gICAgICAgICAge2I+Pj49KDMpO2stPSgzKTt9XG5cbiAgICAgICAgICB0aGlzLm1vZGUgPSBJQl9UQUJMRTtcbiAgICAgICAgICBicmVhaztcbiAgICAgICAgY2FzZSAzOiAgICAgICAgICAgICAgICAgICAgICAgICAvLyBpbGxlZ2FsXG5cbiAgICAgICAgICB7Yj4+Pj0oMyk7ay09KDMpO31cbiAgICAgICAgICB0aGlzLm1vZGUgPSBCQUQ7XG4gICAgICAgICAgei5tc2cgPSBcImludmFsaWQgYmxvY2sgdHlwZVwiO1xuICAgICAgICAgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB0aGlzLndyaXRlPXE7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHR9XG5cdGJyZWFrO1xuICAgICAgY2FzZSBJQl9MRU5TOlxuXHR3aGlsZShrPCgzMikpe1xuXHQgIGlmKG4hPTApe1xuXHQgICAgcj1aX09LO1xuXHQgIH1cblx0ICBlbHNle1xuXHQgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgIHouYXZhaWxfaW49bjtcblx0ICAgIHoudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH07XG5cdCAgbi0tO1xuXHQgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0aWYgKCgoKH5iKSA+Pj4gMTYpICYgMHhmZmZmKSAhPSAoYiAmIDB4ZmZmZikpe1xuXHQgIHRoaXMubW9kZSA9IEJBRDtcblx0ICB6Lm1zZyA9IFwiaW52YWxpZCBzdG9yZWQgYmxvY2sgbGVuZ3Roc1wiO1xuXHQgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB0aGlzLndyaXRlPXE7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHR9XG5cdHRoaXMubGVmdCA9IChiICYgMHhmZmZmKTtcblx0YiA9IGsgPSAwOyAgICAgICAgICAgICAgICAgICAgICAgLy8gZHVtcCBiaXRzXG5cdHRoaXMubW9kZSA9IHRoaXMubGVmdCE9MCA/IElCX1NUT1JFRCA6ICh0aGlzLmxhc3QhPTAgPyBJQl9EUlkgOiBJQl9UWVBFKTtcblx0YnJlYWs7XG4gICAgICBjYXNlIElCX1NUT1JFRDpcblx0aWYgKG4gPT0gMCl7XG5cdCAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgd3JpdGU9cTtcblx0ICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdH1cblxuXHRpZihtPT0wKXtcblx0ICBpZihxPT1lbmQmJnJlYWQhPTApe1xuXHQgICAgcT0wOyBtPShxPHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTtcblx0ICB9XG5cdCAgaWYobT09MCl7XG5cdCAgICB0aGlzLndyaXRlPXE7IFxuXHQgICAgcj10aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgIHE9dGhpcy53cml0ZTsgbSA9IChxIDwgdGhpcy5yZWFkID8gdGhpcy5yZWFkLXEtMSA6IHRoaXMuZW5kLXEpO1xuXHQgICAgaWYocT09dGhpcy5lbmQgJiYgdGhpcy5yZWFkICE9IDApe1xuXHQgICAgICBxPTA7IG0gPSAocSA8IHRoaXMucmVhZCA/IHRoaXMucmVhZC1xLTEgOiB0aGlzLmVuZC1xKTtcblx0ICAgIH1cblx0ICAgIGlmKG09PTApe1xuXHQgICAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICAgIHRoaXMud3JpdGU9cTtcblx0ICAgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgfVxuXHQgIH1cblx0fVxuXHRyPVpfT0s7XG5cblx0dCA9IHRoaXMubGVmdDtcblx0aWYodD5uKSB0ID0gbjtcblx0aWYodD5tKSB0ID0gbTtcblx0YXJyYXlDb3B5KHoubmV4dF9pbiwgcCwgdGhpcy53aW5kb3csIHEsIHQpO1xuXHRwICs9IHQ7ICBuIC09IHQ7XG5cdHEgKz0gdDsgIG0gLT0gdDtcblx0aWYgKCh0aGlzLmxlZnQgLT0gdCkgIT0gMClcblx0ICBicmVhaztcblx0dGhpcy5tb2RlID0gKHRoaXMubGFzdCAhPSAwID8gSUJfRFJZIDogSUJfVFlQRSk7XG5cdGJyZWFrO1xuICAgICAgY2FzZSBJQl9UQUJMRTpcblxuXHR3aGlsZShrPCgxNCkpe1xuXHQgIGlmKG4hPTApe1xuXHQgICAgcj1aX09LO1xuXHQgIH1cblx0ICBlbHNle1xuXHQgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgIHouYXZhaWxfaW49bjtcblx0ICAgIHoudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH07XG5cdCAgbi0tO1xuXHQgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0dGhpcy50YWJsZSA9IHQgPSAoYiAmIDB4M2ZmZik7XG5cdGlmICgodCAmIDB4MWYpID4gMjkgfHwgKCh0ID4+IDUpICYgMHgxZikgPiAyOSlcblx0ICB7XG5cdCAgICB0aGlzLm1vZGUgPSBJQl9CQUQ7XG5cdCAgICB6Lm1zZyA9IFwidG9vIG1hbnkgbGVuZ3RoIG9yIGRpc3RhbmNlIHN5bWJvbHNcIjtcblx0ICAgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICAgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICB0aGlzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfVxuXHR0ID0gMjU4ICsgKHQgJiAweDFmKSArICgodCA+PiA1KSAmIDB4MWYpO1xuXHRpZih0aGlzLmJsZW5zPT1udWxsIHx8IHRoaXMuYmxlbnMubGVuZ3RoPHQpe1xuXHQgICAgdGhpcy5ibGVucz1uZXcgSW50MzJBcnJheSh0KTtcblx0fVxuXHRlbHNle1xuXHQgIGZvcih2YXIgaT0wOyBpPHQ7IGkrKyl7XG4gICAgICAgICAgICAgIHRoaXMuYmxlbnNbaV09MDtcbiAgICAgICAgICB9XG5cdH1cblxuXHR7Yj4+Pj0oMTQpO2stPSgxNCk7fVxuXG5cdHRoaXMuaW5kZXggPSAwO1xuXHRtb2RlID0gSUJfQlRSRUU7XG4gICAgICBjYXNlIElCX0JUUkVFOlxuXHR3aGlsZSAodGhpcy5pbmRleCA8IDQgKyAodGhpcy50YWJsZSA+Pj4gMTApKXtcblx0ICB3aGlsZShrPCgzKSl7XG5cdCAgICBpZihuIT0wKXtcblx0ICAgICAgcj1aX09LO1xuXHQgICAgfVxuXHQgICAgZWxzZXtcblx0ICAgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgICAgei5hdmFpbF9pbj1uO1xuXHQgICAgICB6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICB9O1xuXHQgICAgbi0tO1xuXHQgICAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgICAgays9ODtcblx0ICB9XG5cblx0ICB0aGlzLmJsZW5zW0lORkJMT0NLU19CT1JERVJbdGhpcy5pbmRleCsrXV0gPSBiJjc7XG5cblx0ICB7Yj4+Pj0oMyk7ay09KDMpO31cblx0fVxuXG5cdHdoaWxlKHRoaXMuaW5kZXggPCAxOSl7XG5cdCAgdGhpcy5ibGVuc1tJTkZCTE9DS1NfQk9SREVSW3RoaXMuaW5kZXgrK11dID0gMDtcblx0fVxuXG5cdHRoaXMuYmJbMF0gPSA3O1xuXHR0ID0gdGhpcy5pbmZ0cmVlLmluZmxhdGVfdHJlZXNfYml0cyh0aGlzLmJsZW5zLCB0aGlzLmJiLCB0aGlzLnRiLCB0aGlzLmh1ZnRzLCB6KTtcblx0aWYgKHQgIT0gWl9PSyl7XG5cdCAgciA9IHQ7XG5cdCAgaWYgKHIgPT0gWl9EQVRBX0VSUk9SKXtcblx0ICAgIHRoaXMuYmxlbnM9bnVsbDtcblx0ICAgIHRoaXMubW9kZSA9IElCX0JBRDtcblx0ICB9XG5cblx0ICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICB3cml0ZT1xO1xuXHQgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0fVxuXG5cdHRoaXMuaW5kZXggPSAwO1xuXHR0aGlzLm1vZGUgPSBJQl9EVFJFRTtcbiAgICAgIGNhc2UgSUJfRFRSRUU6XG5cdHdoaWxlICh0cnVlKXtcblx0ICB0ID0gdGhpcy50YWJsZTtcblx0ICBpZighKHRoaXMuaW5kZXggPCAyNTggKyAodCAmIDB4MWYpICsgKCh0ID4+IDUpICYgMHgxZikpKXtcblx0ICAgIGJyZWFrO1xuXHQgIH1cblxuXHQgIHZhciBoOyAvL2ludFtdXG5cdCAgdmFyIGksIGosIGM7XG5cblx0ICB0ID0gdGhpcy5iYlswXTtcblxuXHQgIHdoaWxlKGs8KHQpKXtcblx0ICAgIGlmKG4hPTApe1xuXHQgICAgICByPVpfT0s7XG5cdCAgICB9XG5cdCAgICBlbHNle1xuXHQgICAgICB0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHQgICAgICB6LmF2YWlsX2luPW47XG5cdCAgICAgIHoudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgICB0aGlzLndyaXRlPXE7XG5cdCAgICAgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgIH07XG5cdCAgICBuLS07XG5cdCAgICBifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7XG5cdCAgICBrKz04O1xuXHQgIH1cblxuLy9cdCAgaWYgKHRoaXMudGJbMF09PS0xKXtcbi8vICAgICAgICAgICAgZGxvZyhcIm51bGwuLi5cIik7XG4vL1x0ICB9XG5cblx0ICB0PXRoaXMuaHVmdHNbKHRoaXMudGJbMF0rKGIgJiBpbmZsYXRlX21hc2tbdF0pKSozKzFdO1xuXHQgIGM9dGhpcy5odWZ0c1sodGhpcy50YlswXSsoYiAmIGluZmxhdGVfbWFza1t0XSkpKjMrMl07XG5cblx0ICBpZiAoYyA8IDE2KXtcblx0ICAgIGI+Pj49KHQpO2stPSh0KTtcblx0ICAgIHRoaXMuYmxlbnNbdGhpcy5pbmRleCsrXSA9IGM7XG5cdCAgfVxuXHQgIGVsc2UgeyAvLyBjID09IDE2Li4xOFxuXHQgICAgaSA9IGMgPT0gMTggPyA3IDogYyAtIDE0O1xuXHQgICAgaiA9IGMgPT0gMTggPyAxMSA6IDM7XG5cblx0ICAgIHdoaWxlKGs8KHQraSkpe1xuXHQgICAgICBpZihuIT0wKXtcblx0XHRyPVpfT0s7XG5cdCAgICAgIH1cblx0ICAgICAgZWxzZXtcblx0XHR0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHRcdHouYXZhaWxfaW49bjtcblx0XHR6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0XHR0aGlzLndyaXRlPXE7XG5cdFx0cmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgICB9O1xuXHQgICAgICBuLS07XG5cdCAgICAgIGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICAgICAgays9ODtcblx0ICAgIH1cblxuXHQgICAgYj4+Pj0odCk7ay09KHQpO1xuXG5cdCAgICBqICs9IChiICYgaW5mbGF0ZV9tYXNrW2ldKTtcblxuXHQgICAgYj4+Pj0oaSk7ay09KGkpO1xuXG5cdCAgICBpID0gdGhpcy5pbmRleDtcblx0ICAgIHQgPSB0aGlzLnRhYmxlO1xuXHQgICAgaWYgKGkgKyBqID4gMjU4ICsgKHQgJiAweDFmKSArICgodCA+PiA1KSAmIDB4MWYpIHx8XG5cdFx0KGMgPT0gMTYgJiYgaSA8IDEpKXtcblx0ICAgICAgdGhpcy5ibGVucz1udWxsO1xuXHQgICAgICB0aGlzLm1vZGUgPSBJQl9CQUQ7XG5cdCAgICAgIHoubXNnID0gXCJpbnZhbGlkIGJpdCBsZW5ndGggcmVwZWF0XCI7XG5cdCAgICAgIHIgPSBaX0RBVEFfRVJST1I7XG5cblx0ICAgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgICB0aGlzLndyaXRlPXE7XG5cdCAgICAgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgIH1cblxuXHQgICAgYyA9IGMgPT0gMTYgPyB0aGlzLmJsZW5zW2ktMV0gOiAwO1xuXHQgICAgZG97XG5cdCAgICAgIHRoaXMuYmxlbnNbaSsrXSA9IGM7XG5cdCAgICB9XG5cdCAgICB3aGlsZSAoLS1qIT0wKTtcblx0ICAgIHRoaXMuaW5kZXggPSBpO1xuXHQgIH1cblx0fVxuXG5cdHRoaXMudGJbMF09LTE7XG5cdHtcblx0ICAgIHZhciBibD1uZXcgSW50MzJBcnJheSgxKTtcblx0ICAgIHZhciBiZD1uZXcgSW50MzJBcnJheSgxKTtcblx0ICAgIHZhciB0bD1uZXcgSW50MzJBcnJheSgxKTtcblx0ICAgIHZhciB0ZD1uZXcgSW50MzJBcnJheSgxKTtcblx0ICAgIGJsWzBdID0gOTsgICAgICAgICAvLyBtdXN0IGJlIDw9IDkgZm9yIGxvb2thaGVhZCBhc3N1bXB0aW9uc1xuXHQgICAgYmRbMF0gPSA2OyAgICAgICAgIC8vIG11c3QgYmUgPD0gOSBmb3IgbG9va2FoZWFkIGFzc3VtcHRpb25zXG5cblx0ICAgIHQgPSB0aGlzLnRhYmxlO1xuXHQgICAgdCA9IHRoaXMuaW5mdHJlZS5pbmZsYXRlX3RyZWVzX2R5bmFtaWMoMjU3ICsgKHQgJiAweDFmKSwgXG5cdFx0XHRcdFx0ICAgICAgMSArICgodCA+PiA1KSAmIDB4MWYpLFxuXHRcdFx0XHRcdCAgICAgIHRoaXMuYmxlbnMsIGJsLCBiZCwgdGwsIHRkLCB0aGlzLmh1ZnRzLCB6KTtcblxuXHQgICAgaWYgKHQgIT0gWl9PSyl7XG5cdCAgICAgICAgaWYgKHQgPT0gWl9EQVRBX0VSUk9SKXtcblx0ICAgICAgICAgICAgdGhpcy5ibGVucz1udWxsO1xuXHQgICAgICAgICAgICB0aGlzLm1vZGUgPSBCQUQ7XG5cdCAgICAgICAgfVxuXHQgICAgICAgIHIgPSB0O1xuXG5cdCAgICAgICAgdGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ICAgICAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICAgICAgdGhpcy53cml0ZT1xO1xuXHQgICAgICAgIHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgIH1cblx0ICAgIHRoaXMuY29kZXMuaW5pdChibFswXSwgYmRbMF0sIHRoaXMuaHVmdHMsIHRsWzBdLCB0aGlzLmh1ZnRzLCB0ZFswXSwgeik7XG5cdH1cblx0dGhpcy5tb2RlID0gSUJfQ09ERVM7XG4gICAgICBjYXNlIElCX0NPREVTOlxuXHR0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7XG5cdHouYXZhaWxfaW49bjsgei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHRoaXMud3JpdGU9cTtcblxuXHRpZiAoKHIgPSB0aGlzLmNvZGVzLnByb2ModGhpcywgeiwgcikpICE9IFpfU1RSRUFNX0VORCl7XG5cdCAgcmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LCByKTtcblx0fVxuXHRyID0gWl9PSztcblx0dGhpcy5jb2Rlcy5mcmVlKHopO1xuXG5cdHA9ei5uZXh0X2luX2luZGV4OyBuPXouYXZhaWxfaW47Yj10aGlzLmJpdGI7az10aGlzLmJpdGs7XG5cdHE9dGhpcy53cml0ZTttID0gKHEgPCB0aGlzLnJlYWQgPyB0aGlzLnJlYWQtcS0xIDogdGhpcy5lbmQtcSk7XG5cblx0aWYgKHRoaXMubGFzdD09MCl7XG5cdCAgdGhpcy5tb2RlID0gSUJfVFlQRTtcblx0ICBicmVhaztcblx0fVxuXHR0aGlzLm1vZGUgPSBJQl9EUlk7XG4gICAgICBjYXNlIElCX0RSWTpcblx0dGhpcy53cml0ZT1xOyBcblx0ciA9IHRoaXMuaW5mbGF0ZV9mbHVzaCh6LCByKTsgXG5cdHE9dGhpcy53cml0ZTsgbSA9IChxIDwgdGhpcy5yZWFkID8gdGhpcy5yZWFkLXEtMSA6IHRoaXMuZW5kLXEpO1xuXHRpZiAodGhpcy5yZWFkICE9IHRoaXMud3JpdGUpe1xuXHQgIHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdCAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgIHRoaXMud3JpdGU9cTtcblx0ICByZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHosIHIpO1xuXHR9XG5cdG1vZGUgPSBET05FO1xuICAgICAgY2FzZSBJQl9ET05FOlxuXHRyID0gWl9TVFJFQU1fRU5EO1xuXG5cdHRoaXMuYml0Yj1iOyB0aGlzLmJpdGs9azsgXG5cdHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0dGhpcy53cml0ZT1xO1xuXHRyZXR1cm4gdGhpcy5pbmZsYXRlX2ZsdXNoKHosIHIpO1xuICAgICAgY2FzZSBJQl9CQUQ6XG5cdHIgPSBaX0RBVEFfRVJST1I7XG5cblx0dGhpcy5iaXRiPWI7IHRoaXMuYml0az1rOyBcblx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHR0aGlzLndyaXRlPXE7XG5cdHJldHVybiB0aGlzLmluZmxhdGVfZmx1c2goeiwgcik7XG5cbiAgICAgIGRlZmF1bHQ6XG5cdHIgPSBaX1NUUkVBTV9FUlJPUjtcblxuXHR0aGlzLmJpdGI9YjsgdGhpcy5iaXRrPWs7IFxuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHRoaXMud3JpdGU9cTtcblx0cmV0dXJuIHRoaXMuaW5mbGF0ZV9mbHVzaCh6LCByKTtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuSW5mQmxvY2tzLnByb3RvdHlwZS5mcmVlID0gZnVuY3Rpb24oeil7XG4gICAgdGhpcy5yZXNldCh6LCBudWxsKTtcbiAgICB0aGlzLndpbmRvdz1udWxsO1xuICAgIHRoaXMuaHVmdHM9bnVsbDtcbn1cblxuSW5mQmxvY2tzLnByb3RvdHlwZS5zZXRfZGljdGlvbmFyeSA9IGZ1bmN0aW9uKGQsIHN0YXJ0LCBuKXtcbiAgICBhcnJheUNvcHkoZCwgc3RhcnQsIHdpbmRvdywgMCwgbik7XG4gICAgdGhpcy5yZWFkID0gdGhpcy53cml0ZSA9IG47XG59XG5cbiAgLy8gUmV0dXJucyB0cnVlIGlmIGluZmxhdGUgaXMgY3VycmVudGx5IGF0IHRoZSBlbmQgb2YgYSBibG9jayBnZW5lcmF0ZWRcbiAgLy8gYnkgWl9TWU5DX0ZMVVNIIG9yIFpfRlVMTF9GTFVTSC4gXG5JbmZCbG9ja3MucHJvdG90eXBlLnN5bmNfcG9pbnQgPSBmdW5jdGlvbigpe1xuICAgIHJldHVybiB0aGlzLm1vZGUgPT0gSUJfTEVOUztcbn1cblxuICAvLyBjb3B5IGFzIG11Y2ggYXMgcG9zc2libGUgZnJvbSB0aGUgc2xpZGluZyB3aW5kb3cgdG8gdGhlIG91dHB1dCBhcmVhXG5JbmZCbG9ja3MucHJvdG90eXBlLmluZmxhdGVfZmx1c2ggPSBmdW5jdGlvbih6LCByKXtcbiAgICB2YXIgbjtcbiAgICB2YXIgcDtcbiAgICB2YXIgcTtcblxuICAgIC8vIGxvY2FsIGNvcGllcyBvZiBzb3VyY2UgYW5kIGRlc3RpbmF0aW9uIHBvaW50ZXJzXG4gICAgcCA9IHoubmV4dF9vdXRfaW5kZXg7XG4gICAgcSA9IHRoaXMucmVhZDtcblxuICAgIC8vIGNvbXB1dGUgbnVtYmVyIG9mIGJ5dGVzIHRvIGNvcHkgYXMgZmFyIGFzIGVuZCBvZiB3aW5kb3dcbiAgICBuID0gKChxIDw9IHRoaXMud3JpdGUgPyB0aGlzLndyaXRlIDogdGhpcy5lbmQpIC0gcSk7XG4gICAgaWYgKG4gPiB6LmF2YWlsX291dCkgbiA9IHouYXZhaWxfb3V0O1xuICAgIGlmIChuIT0wICYmIHIgPT0gWl9CVUZfRVJST1IpIHIgPSBaX09LO1xuXG4gICAgLy8gdXBkYXRlIGNvdW50ZXJzXG4gICAgei5hdmFpbF9vdXQgLT0gbjtcbiAgICB6LnRvdGFsX291dCArPSBuO1xuXG4gICAgLy8gdXBkYXRlIGNoZWNrIGluZm9ybWF0aW9uXG4gICAgaWYodGhpcy5jaGVja2ZuICE9IG51bGwpXG4gICAgICB6LmFkbGVyPXRoaXMuY2hlY2s9ei5fYWRsZXIuYWRsZXIzMih0aGlzLmNoZWNrLCB0aGlzLndpbmRvdywgcSwgbik7XG5cbiAgICAvLyBjb3B5IGFzIGZhciBhcyBlbmQgb2Ygd2luZG93XG4gICAgYXJyYXlDb3B5KHRoaXMud2luZG93LCBxLCB6Lm5leHRfb3V0LCBwLCBuKTtcbiAgICBwICs9IG47XG4gICAgcSArPSBuO1xuXG4gICAgLy8gc2VlIGlmIG1vcmUgdG8gY29weSBhdCBiZWdpbm5pbmcgb2Ygd2luZG93XG4gICAgaWYgKHEgPT0gdGhpcy5lbmQpe1xuICAgICAgLy8gd3JhcCBwb2ludGVyc1xuICAgICAgcSA9IDA7XG4gICAgICBpZiAodGhpcy53cml0ZSA9PSB0aGlzLmVuZClcbiAgICAgICAgdGhpcy53cml0ZSA9IDA7XG5cbiAgICAgIC8vIGNvbXB1dGUgYnl0ZXMgdG8gY29weVxuICAgICAgbiA9IHRoaXMud3JpdGUgLSBxO1xuICAgICAgaWYgKG4gPiB6LmF2YWlsX291dCkgbiA9IHouYXZhaWxfb3V0O1xuICAgICAgaWYgKG4hPTAgJiYgciA9PSBaX0JVRl9FUlJPUikgciA9IFpfT0s7XG5cbiAgICAgIC8vIHVwZGF0ZSBjb3VudGVyc1xuICAgICAgei5hdmFpbF9vdXQgLT0gbjtcbiAgICAgIHoudG90YWxfb3V0ICs9IG47XG5cbiAgICAgIC8vIHVwZGF0ZSBjaGVjayBpbmZvcm1hdGlvblxuICAgICAgaWYodGhpcy5jaGVja2ZuICE9IG51bGwpXG5cdHouYWRsZXI9dGhpcy5jaGVjaz16Ll9hZGxlci5hZGxlcjMyKHRoaXMuY2hlY2ssIHRoaXMud2luZG93LCBxLCBuKTtcblxuICAgICAgLy8gY29weVxuICAgICAgYXJyYXlDb3B5KHRoaXMud2luZG93LCBxLCB6Lm5leHRfb3V0LCBwLCBuKTtcbiAgICAgIHAgKz0gbjtcbiAgICAgIHEgKz0gbjtcbiAgICB9XG5cbiAgICAvLyB1cGRhdGUgcG9pbnRlcnNcbiAgICB6Lm5leHRfb3V0X2luZGV4ID0gcDtcbiAgICB0aGlzLnJlYWQgPSBxO1xuXG4gICAgLy8gZG9uZVxuICAgIHJldHVybiByO1xuICB9XG5cbi8vXG4vLyBJbmZDb2Rlcy5qYXZhXG4vL1xuXG52YXIgSUNfU1RBUlQ9MDsgIC8vIHg6IHNldCB1cCBmb3IgTEVOXG52YXIgSUNfTEVOPTE7ICAgIC8vIGk6IGdldCBsZW5ndGgvbGl0ZXJhbC9lb2IgbmV4dFxudmFyIElDX0xFTkVYVD0yOyAvLyBpOiBnZXR0aW5nIGxlbmd0aCBleHRyYSAoaGF2ZSBiYXNlKVxudmFyIElDX0RJU1Q9MzsgICAvLyBpOiBnZXQgZGlzdGFuY2UgbmV4dFxudmFyIElDX0RJU1RFWFQ9NDsvLyBpOiBnZXR0aW5nIGRpc3RhbmNlIGV4dHJhXG52YXIgSUNfQ09QWT01OyAgIC8vIG86IGNvcHlpbmcgYnl0ZXMgaW4gd2luZG93LCB3YWl0aW5nIGZvciBzcGFjZVxudmFyIElDX0xJVD02OyAgICAvLyBvOiBnb3QgbGl0ZXJhbCwgd2FpdGluZyBmb3Igb3V0cHV0IHNwYWNlXG52YXIgSUNfV0FTSD03OyAgIC8vIG86IGdvdCBlb2IsIHBvc3NpYmx5IHN0aWxsIG91dHB1dCB3YWl0aW5nXG52YXIgSUNfRU5EPTg7ICAgIC8vIHg6IGdvdCBlb2IgYW5kIGFsbCBkYXRhIGZsdXNoZWRcbnZhciBJQ19CQURDT0RFPTk7Ly8geDogZ290IGVycm9yXG5cbmZ1bmN0aW9uIEluZkNvZGVzKCkge1xufVxuXG5JbmZDb2Rlcy5wcm90b3R5cGUuaW5pdCA9IGZ1bmN0aW9uKGJsLCBiZCwgdGwsIHRsX2luZGV4LCB0ZCwgdGRfaW5kZXgsIHopIHtcbiAgICB0aGlzLm1vZGU9SUNfU1RBUlQ7XG4gICAgdGhpcy5sYml0cz1ibDtcbiAgICB0aGlzLmRiaXRzPWJkO1xuICAgIHRoaXMubHRyZWU9dGw7XG4gICAgdGhpcy5sdHJlZV9pbmRleD10bF9pbmRleDtcbiAgICB0aGlzLmR0cmVlID0gdGQ7XG4gICAgdGhpcy5kdHJlZV9pbmRleD10ZF9pbmRleDtcbiAgICB0aGlzLnRyZWU9bnVsbDtcbn1cblxuSW5mQ29kZXMucHJvdG90eXBlLnByb2MgPSBmdW5jdGlvbihzLCB6LCByKXsgXG4gICAgdmFyIGo7ICAgICAgICAgICAgICAvLyB0ZW1wb3Jhcnkgc3RvcmFnZVxuICAgIHZhciB0OyAgICAgICAgICAgICAgLy8gdGVtcG9yYXJ5IHBvaW50ZXIgKGludFtdKVxuICAgIHZhciB0aW5kZXg7ICAgICAgICAgLy8gdGVtcG9yYXJ5IHBvaW50ZXJcbiAgICB2YXIgZTsgICAgICAgICAgICAgIC8vIGV4dHJhIGJpdHMgb3Igb3BlcmF0aW9uXG4gICAgdmFyIGI9MDsgICAgICAgICAgICAvLyBiaXQgYnVmZmVyXG4gICAgdmFyIGs9MDsgICAgICAgICAgICAvLyBiaXRzIGluIGJpdCBidWZmZXJcbiAgICB2YXIgcD0wOyAgICAgICAgICAgIC8vIGlucHV0IGRhdGEgcG9pbnRlclxuICAgIHZhciBuOyAgICAgICAgICAgICAgLy8gYnl0ZXMgYXZhaWxhYmxlIHRoZXJlXG4gICAgdmFyIHE7ICAgICAgICAgICAgICAvLyBvdXRwdXQgd2luZG93IHdyaXRlIHBvaW50ZXJcbiAgICB2YXIgbTsgICAgICAgICAgICAgIC8vIGJ5dGVzIHRvIGVuZCBvZiB3aW5kb3cgb3IgcmVhZCBwb2ludGVyXG4gICAgdmFyIGY7ICAgICAgICAgICAgICAvLyBwb2ludGVyIHRvIGNvcHkgc3RyaW5ncyBmcm9tXG5cbiAgICAvLyBjb3B5IGlucHV0L291dHB1dCBpbmZvcm1hdGlvbiB0byBsb2NhbHMgKFVQREFURSBtYWNybyByZXN0b3JlcylcbiAgICBwPXoubmV4dF9pbl9pbmRleDtuPXouYXZhaWxfaW47Yj1zLmJpdGI7az1zLmJpdGs7XG4gICAgcT1zLndyaXRlO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO1xuXG4gICAgLy8gcHJvY2VzcyBpbnB1dCBhbmQgb3V0cHV0IGJhc2VkIG9uIGN1cnJlbnQgc3RhdGVcbiAgICB3aGlsZSAodHJ1ZSl7XG4gICAgICBzd2l0Y2ggKHRoaXMubW9kZSl7XG5cdC8vIHdhaXRpbmcgZm9yIFwiaTpcIj1pbnB1dCwgXCJvOlwiPW91dHB1dCwgXCJ4OlwiPW5vdGhpbmdcbiAgICAgIGNhc2UgSUNfU1RBUlQ6ICAgICAgICAgLy8geDogc2V0IHVwIGZvciBMRU5cblx0aWYgKG0gPj0gMjU4ICYmIG4gPj0gMTApe1xuXG5cdCAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgIHMud3JpdGU9cTtcblx0ICByID0gdGhpcy5pbmZsYXRlX2Zhc3QodGhpcy5sYml0cywgdGhpcy5kYml0cywgXG5cdFx0XHQgICB0aGlzLmx0cmVlLCB0aGlzLmx0cmVlX2luZGV4LCBcblx0XHRcdCAgIHRoaXMuZHRyZWUsIHRoaXMuZHRyZWVfaW5kZXgsXG5cdFx0XHQgICBzLCB6KTtcblxuXHQgIHA9ei5uZXh0X2luX2luZGV4O249ei5hdmFpbF9pbjtiPXMuYml0YjtrPXMuYml0aztcblx0ICBxPXMud3JpdGU7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7XG5cblx0ICBpZiAociAhPSBaX09LKXtcblx0ICAgIHRoaXMubW9kZSA9IHIgPT0gWl9TVFJFQU1fRU5EID8gSUNfV0FTSCA6IElDX0JBRENPREU7XG5cdCAgICBicmVhaztcblx0ICB9XG5cdH1cblx0dGhpcy5uZWVkID0gdGhpcy5sYml0cztcblx0dGhpcy50cmVlID0gdGhpcy5sdHJlZTtcblx0dGhpcy50cmVlX2luZGV4PXRoaXMubHRyZWVfaW5kZXg7XG5cblx0dGhpcy5tb2RlID0gSUNfTEVOO1xuICAgICAgY2FzZSBJQ19MRU46ICAgICAgICAgICAvLyBpOiBnZXQgbGVuZ3RoL2xpdGVyYWwvZW9iIG5leHRcblx0aiA9IHRoaXMubmVlZDtcblxuXHR3aGlsZShrPChqKSl7XG5cdCAgaWYobiE9MClyPVpfT0s7XG5cdCAgZWxzZXtcblxuXHQgICAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICBzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfVxuXHQgIG4tLTtcblx0ICBifD0oei5uZXh0X2luW3ArK10mMHhmZik8PGs7XG5cdCAgays9ODtcblx0fVxuXG5cdHRpbmRleD0odGhpcy50cmVlX2luZGV4KyhiJmluZmxhdGVfbWFza1tqXSkpKjM7XG5cblx0Yj4+Pj0odGhpcy50cmVlW3RpbmRleCsxXSk7XG5cdGstPSh0aGlzLnRyZWVbdGluZGV4KzFdKTtcblxuXHRlPXRoaXMudHJlZVt0aW5kZXhdO1xuXG5cdGlmKGUgPT0gMCl7ICAgICAgICAgICAgICAgLy8gbGl0ZXJhbFxuXHQgIHRoaXMubGl0ID0gdGhpcy50cmVlW3RpbmRleCsyXTtcblx0ICB0aGlzLm1vZGUgPSBJQ19MSVQ7XG5cdCAgYnJlYWs7XG5cdH1cblx0aWYoKGUgJiAxNikhPTAgKXsgICAgICAgICAgLy8gbGVuZ3RoXG5cdCAgdGhpcy5nZXQgPSBlICYgMTU7XG5cdCAgdGhpcy5sZW4gPSB0aGlzLnRyZWVbdGluZGV4KzJdO1xuXHQgIHRoaXMubW9kZSA9IElDX0xFTkVYVDtcblx0ICBicmVhaztcblx0fVxuXHRpZiAoKGUgJiA2NCkgPT0gMCl7ICAgICAgICAvLyBuZXh0IHRhYmxlXG5cdCAgdGhpcy5uZWVkID0gZTtcblx0ICB0aGlzLnRyZWVfaW5kZXggPSB0aW5kZXgvMyArIHRoaXMudHJlZVt0aW5kZXgrMl07XG5cdCAgYnJlYWs7XG5cdH1cblx0aWYgKChlICYgMzIpIT0wKXsgICAgICAgICAgICAgICAvLyBlbmQgb2YgYmxvY2tcblx0ICB0aGlzLm1vZGUgPSBJQ19XQVNIO1xuXHQgIGJyZWFrO1xuXHR9XG5cdHRoaXMubW9kZSA9IElDX0JBRENPREU7ICAgICAgICAvLyBpbnZhbGlkIGNvZGVcblx0ei5tc2cgPSBcImludmFsaWQgbGl0ZXJhbC9sZW5ndGggY29kZVwiO1xuXHRyID0gWl9EQVRBX0VSUk9SO1xuXG5cdHMuYml0Yj1iO3MuYml0az1rO1xuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHMud3JpdGU9cTtcblx0cmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXG4gICAgICBjYXNlIElDX0xFTkVYVDogICAgICAgIC8vIGk6IGdldHRpbmcgbGVuZ3RoIGV4dHJhIChoYXZlIGJhc2UpXG5cdGogPSB0aGlzLmdldDtcblxuXHR3aGlsZShrPChqKSl7XG5cdCAgaWYobiE9MClyPVpfT0s7XG5cdCAgZWxzZXtcblxuXHQgICAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICBzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfVxuXHQgIG4tLTsgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgIGsrPTg7XG5cdH1cblxuXHR0aGlzLmxlbiArPSAoYiAmIGluZmxhdGVfbWFza1tqXSk7XG5cblx0Yj4+PWo7XG5cdGstPWo7XG5cblx0dGhpcy5uZWVkID0gdGhpcy5kYml0cztcblx0dGhpcy50cmVlID0gdGhpcy5kdHJlZTtcblx0dGhpcy50cmVlX2luZGV4ID0gdGhpcy5kdHJlZV9pbmRleDtcblx0dGhpcy5tb2RlID0gSUNfRElTVDtcbiAgICAgIGNhc2UgSUNfRElTVDogICAgICAgICAgLy8gaTogZ2V0IGRpc3RhbmNlIG5leHRcblx0aiA9IHRoaXMubmVlZDtcblxuXHR3aGlsZShrPChqKSl7XG5cdCAgaWYobiE9MClyPVpfT0s7XG5cdCAgZWxzZXtcblxuXHQgICAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgICBzLndyaXRlPXE7XG5cdCAgICByZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgfVxuXHQgIG4tLTsgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO1xuXHQgIGsrPTg7XG5cdH1cblxuXHR0aW5kZXg9KHRoaXMudHJlZV9pbmRleCsoYiAmIGluZmxhdGVfbWFza1tqXSkpKjM7XG5cblx0Yj4+PXRoaXMudHJlZVt0aW5kZXgrMV07XG5cdGstPXRoaXMudHJlZVt0aW5kZXgrMV07XG5cblx0ZSA9ICh0aGlzLnRyZWVbdGluZGV4XSk7XG5cdGlmKChlICYgMTYpIT0wKXsgICAgICAgICAgICAgICAvLyBkaXN0YW5jZVxuXHQgIHRoaXMuZ2V0ID0gZSAmIDE1O1xuXHQgIHRoaXMuZGlzdCA9IHRoaXMudHJlZVt0aW5kZXgrMl07XG5cdCAgdGhpcy5tb2RlID0gSUNfRElTVEVYVDtcblx0ICBicmVhaztcblx0fVxuXHRpZiAoKGUgJiA2NCkgPT0gMCl7ICAgICAgICAvLyBuZXh0IHRhYmxlXG5cdCAgdGhpcy5uZWVkID0gZTtcblx0ICB0aGlzLnRyZWVfaW5kZXggPSB0aW5kZXgvMyArIHRoaXMudHJlZVt0aW5kZXgrMl07XG5cdCAgYnJlYWs7XG5cdH1cblx0dGhpcy5tb2RlID0gSUNfQkFEQ09ERTsgICAgICAgIC8vIGludmFsaWQgY29kZVxuXHR6Lm1zZyA9IFwiaW52YWxpZCBkaXN0YW5jZSBjb2RlXCI7XG5cdHIgPSBaX0RBVEFfRVJST1I7XG5cblx0cy5iaXRiPWI7cy5iaXRrPWs7XG5cdHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0cy53cml0ZT1xO1xuXHRyZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cbiAgICAgIGNhc2UgSUNfRElTVEVYVDogICAgICAgLy8gaTogZ2V0dGluZyBkaXN0YW5jZSBleHRyYVxuXHRqID0gdGhpcy5nZXQ7XG5cblx0d2hpbGUoazwoaikpe1xuXHQgIGlmKG4hPTApcj1aX09LO1xuXHQgIGVsc2V7XG5cblx0ICAgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgcy53cml0ZT1xO1xuXHQgICAgcmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgIH1cblx0ICBuLS07IGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztcblx0ICBrKz04O1xuXHR9XG5cblx0dGhpcy5kaXN0ICs9IChiICYgaW5mbGF0ZV9tYXNrW2pdKTtcblxuXHRiPj49ajtcblx0ay09ajtcblxuXHR0aGlzLm1vZGUgPSBJQ19DT1BZO1xuICAgICAgY2FzZSBJQ19DT1BZOiAgICAgICAgICAvLyBvOiBjb3B5aW5nIGJ5dGVzIGluIHdpbmRvdywgd2FpdGluZyBmb3Igc3BhY2VcbiAgICAgICAgZiA9IHEgLSB0aGlzLmRpc3Q7XG4gICAgICAgIHdoaWxlKGYgPCAwKXsgICAgIC8vIG1vZHVsbyB3aW5kb3cgc2l6ZS1cIndoaWxlXCIgaW5zdGVhZFxuICAgICAgICAgIGYgKz0gcy5lbmQ7ICAgICAvLyBvZiBcImlmXCIgaGFuZGxlcyBpbnZhbGlkIGRpc3RhbmNlc1xuXHR9XG5cdHdoaWxlICh0aGlzLmxlbiE9MCl7XG5cblx0ICBpZihtPT0wKXtcblx0ICAgIGlmKHE9PXMuZW5kJiZzLnJlYWQhPTApe3E9MDttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTt9XG5cdCAgICBpZihtPT0wKXtcblx0ICAgICAgcy53cml0ZT1xOyByPXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHQgICAgICBxPXMud3JpdGU7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7XG5cblx0ICAgICAgaWYocT09cy5lbmQmJnMucmVhZCE9MCl7cT0wO209cTxzLnJlYWQ/cy5yZWFkLXEtMTpzLmVuZC1xO31cblxuXHQgICAgICBpZihtPT0wKXtcblx0XHRzLmJpdGI9YjtzLmJpdGs9aztcblx0XHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdFx0cy53cml0ZT1xO1xuXHRcdHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcblx0ICAgICAgfSAgXG5cdCAgICB9XG5cdCAgfVxuXG5cdCAgcy53aW5kb3dbcSsrXT1zLndpbmRvd1tmKytdOyBtLS07XG5cblx0ICBpZiAoZiA9PSBzLmVuZClcbiAgICAgICAgICAgIGYgPSAwO1xuXHQgIHRoaXMubGVuLS07XG5cdH1cblx0dGhpcy5tb2RlID0gSUNfU1RBUlQ7XG5cdGJyZWFrO1xuICAgICAgY2FzZSBJQ19MSVQ6ICAgICAgICAgICAvLyBvOiBnb3QgbGl0ZXJhbCwgd2FpdGluZyBmb3Igb3V0cHV0IHNwYWNlXG5cdGlmKG09PTApe1xuXHQgIGlmKHE9PXMuZW5kJiZzLnJlYWQhPTApe3E9MDttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTt9XG5cdCAgaWYobT09MCl7XG5cdCAgICBzLndyaXRlPXE7IHI9cy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICBxPXMud3JpdGU7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7XG5cblx0ICAgIGlmKHE9PXMuZW5kJiZzLnJlYWQhPTApe3E9MDttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTt9XG5cdCAgICBpZihtPT0wKXtcblx0ICAgICAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgICAgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICAgICAgcy53cml0ZT1xO1xuXHQgICAgICByZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cdCAgICB9XG5cdCAgfVxuXHR9XG5cdHI9Wl9PSztcblxuXHRzLndpbmRvd1txKytdPXRoaXMubGl0OyBtLS07XG5cblx0dGhpcy5tb2RlID0gSUNfU1RBUlQ7XG5cdGJyZWFrO1xuICAgICAgY2FzZSBJQ19XQVNIOiAgICAgICAgICAgLy8gbzogZ290IGVvYiwgcG9zc2libHkgbW9yZSBvdXRwdXRcblx0aWYgKGsgPiA3KXsgICAgICAgIC8vIHJldHVybiB1bnVzZWQgYnl0ZSwgaWYgYW55XG5cdCAgayAtPSA4O1xuXHQgIG4rKztcblx0ICBwLS07ICAgICAgICAgICAgIC8vIGNhbiBhbHdheXMgcmV0dXJuIG9uZVxuXHR9XG5cblx0cy53cml0ZT1xOyByPXMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuXHRxPXMud3JpdGU7bT1xPHMucmVhZD9zLnJlYWQtcS0xOnMuZW5kLXE7XG5cblx0aWYgKHMucmVhZCAhPSBzLndyaXRlKXtcblx0ICBzLmJpdGI9YjtzLmJpdGs9aztcblx0ICB6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdCAgcy53cml0ZT1xO1xuXHQgIHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcblx0fVxuXHR0aGlzLm1vZGUgPSBJQ19FTkQ7XG4gICAgICBjYXNlIElDX0VORDpcblx0ciA9IFpfU1RSRUFNX0VORDtcblx0cy5iaXRiPWI7cy5iaXRrPWs7XG5cdHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0cy53cml0ZT1xO1xuXHRyZXR1cm4gcy5pbmZsYXRlX2ZsdXNoKHoscik7XG5cbiAgICAgIGNhc2UgSUNfQkFEQ09ERTogICAgICAgLy8geDogZ290IGVycm9yXG5cblx0ciA9IFpfREFUQV9FUlJPUjtcblxuXHRzLmJpdGI9YjtzLmJpdGs9aztcblx0ei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHRzLndyaXRlPXE7XG5cdHJldHVybiBzLmluZmxhdGVfZmx1c2goeixyKTtcblxuICAgICAgZGVmYXVsdDpcblx0ciA9IFpfU1RSRUFNX0VSUk9SO1xuXG5cdHMuYml0Yj1iO3MuYml0az1rO1xuXHR6LmF2YWlsX2luPW47ei50b3RhbF9pbis9cC16Lm5leHRfaW5faW5kZXg7ei5uZXh0X2luX2luZGV4PXA7XG5cdHMud3JpdGU9cTtcblx0cmV0dXJuIHMuaW5mbGF0ZV9mbHVzaCh6LHIpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG5JbmZDb2Rlcy5wcm90b3R5cGUuZnJlZSA9IGZ1bmN0aW9uKHope1xuICAgIC8vICBaRlJFRSh6LCBjKTtcbn1cblxuICAvLyBDYWxsZWQgd2l0aCBudW1iZXIgb2YgYnl0ZXMgbGVmdCB0byB3cml0ZSBpbiB3aW5kb3cgYXQgbGVhc3QgMjU4XG4gIC8vICh0aGUgbWF4aW11bSBzdHJpbmcgbGVuZ3RoKSBhbmQgbnVtYmVyIG9mIGlucHV0IGJ5dGVzIGF2YWlsYWJsZVxuICAvLyBhdCBsZWFzdCB0ZW4uICBUaGUgdGVuIGJ5dGVzIGFyZSBzaXggYnl0ZXMgZm9yIHRoZSBsb25nZXN0IGxlbmd0aC9cbiAgLy8gZGlzdGFuY2UgcGFpciBwbHVzIGZvdXIgYnl0ZXMgZm9yIG92ZXJsb2FkaW5nIHRoZSBiaXQgYnVmZmVyLlxuXG5JbmZDb2Rlcy5wcm90b3R5cGUuaW5mbGF0ZV9mYXN0ID0gZnVuY3Rpb24oYmwsIGJkLCB0bCwgdGxfaW5kZXgsIHRkLCB0ZF9pbmRleCwgcywgeikge1xuICAgIHZhciB0OyAgICAgICAgICAgICAgICAvLyB0ZW1wb3JhcnkgcG9pbnRlclxuICAgIHZhciAgIHRwOyAgICAgICAgICAgICAvLyB0ZW1wb3JhcnkgcG9pbnRlciAoaW50W10pXG4gICAgdmFyIHRwX2luZGV4OyAgICAgICAgIC8vIHRlbXBvcmFyeSBwb2ludGVyXG4gICAgdmFyIGU7ICAgICAgICAgICAgICAgIC8vIGV4dHJhIGJpdHMgb3Igb3BlcmF0aW9uXG4gICAgdmFyIGI7ICAgICAgICAgICAgICAgIC8vIGJpdCBidWZmZXJcbiAgICB2YXIgazsgICAgICAgICAgICAgICAgLy8gYml0cyBpbiBiaXQgYnVmZmVyXG4gICAgdmFyIHA7ICAgICAgICAgICAgICAgIC8vIGlucHV0IGRhdGEgcG9pbnRlclxuICAgIHZhciBuOyAgICAgICAgICAgICAgICAvLyBieXRlcyBhdmFpbGFibGUgdGhlcmVcbiAgICB2YXIgcTsgICAgICAgICAgICAgICAgLy8gb3V0cHV0IHdpbmRvdyB3cml0ZSBwb2ludGVyXG4gICAgdmFyIG07ICAgICAgICAgICAgICAgIC8vIGJ5dGVzIHRvIGVuZCBvZiB3aW5kb3cgb3IgcmVhZCBwb2ludGVyXG4gICAgdmFyIG1sOyAgICAgICAgICAgICAgIC8vIG1hc2sgZm9yIGxpdGVyYWwvbGVuZ3RoIHRyZWVcbiAgICB2YXIgbWQ7ICAgICAgICAgICAgICAgLy8gbWFzayBmb3IgZGlzdGFuY2UgdHJlZVxuICAgIHZhciBjOyAgICAgICAgICAgICAgICAvLyBieXRlcyB0byBjb3B5XG4gICAgdmFyIGQ7ICAgICAgICAgICAgICAgIC8vIGRpc3RhbmNlIGJhY2sgdG8gY29weSBmcm9tXG4gICAgdmFyIHI7ICAgICAgICAgICAgICAgIC8vIGNvcHkgc291cmNlIHBvaW50ZXJcblxuICAgIHZhciB0cF9pbmRleF90XzM7ICAgICAvLyAodHBfaW5kZXgrdCkqM1xuXG4gICAgLy8gbG9hZCBpbnB1dCwgb3V0cHV0LCBiaXQgdmFsdWVzXG4gICAgcD16Lm5leHRfaW5faW5kZXg7bj16LmF2YWlsX2luO2I9cy5iaXRiO2s9cy5iaXRrO1xuICAgIHE9cy53cml0ZTttPXE8cy5yZWFkP3MucmVhZC1xLTE6cy5lbmQtcTtcblxuICAgIC8vIGluaXRpYWxpemUgbWFza3NcbiAgICBtbCA9IGluZmxhdGVfbWFza1tibF07XG4gICAgbWQgPSBpbmZsYXRlX21hc2tbYmRdO1xuXG4gICAgLy8gZG8gdW50aWwgbm90IGVub3VnaCBpbnB1dCBvciBvdXRwdXQgc3BhY2UgZm9yIGZhc3QgbG9vcFxuICAgIGRvIHsgICAgICAgICAgICAgICAgICAgICAgICAgIC8vIGFzc3VtZSBjYWxsZWQgd2l0aCBtID49IDI1OCAmJiBuID49IDEwXG4gICAgICAvLyBnZXQgbGl0ZXJhbC9sZW5ndGggY29kZVxuICAgICAgd2hpbGUoazwoMjApKXsgICAgICAgICAgICAgIC8vIG1heCBiaXRzIGZvciBsaXRlcmFsL2xlbmd0aCBjb2RlXG5cdG4tLTtcblx0Ynw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO2srPTg7XG4gICAgICB9XG5cbiAgICAgIHQ9IGImbWw7XG4gICAgICB0cD10bDsgXG4gICAgICB0cF9pbmRleD10bF9pbmRleDtcbiAgICAgIHRwX2luZGV4X3RfMz0odHBfaW5kZXgrdCkqMztcbiAgICAgIGlmICgoZSA9IHRwW3RwX2luZGV4X3RfM10pID09IDApe1xuXHRiPj49KHRwW3RwX2luZGV4X3RfMysxXSk7IGstPSh0cFt0cF9pbmRleF90XzMrMV0pO1xuXG5cdHMud2luZG93W3ErK10gPSB0cFt0cF9pbmRleF90XzMrMl07XG5cdG0tLTtcblx0Y29udGludWU7XG4gICAgICB9XG4gICAgICBkbyB7XG5cblx0Yj4+PSh0cFt0cF9pbmRleF90XzMrMV0pOyBrLT0odHBbdHBfaW5kZXhfdF8zKzFdKTtcblxuXHRpZigoZSYxNikhPTApe1xuXHQgIGUgJj0gMTU7XG5cdCAgYyA9IHRwW3RwX2luZGV4X3RfMysyXSArIChiICYgaW5mbGF0ZV9tYXNrW2VdKTtcblxuXHQgIGI+Pj1lOyBrLT1lO1xuXG5cdCAgLy8gZGVjb2RlIGRpc3RhbmNlIGJhc2Ugb2YgYmxvY2sgdG8gY29weVxuXHQgIHdoaWxlKGs8KDE1KSl7ICAgICAgICAgICAvLyBtYXggYml0cyBmb3IgZGlzdGFuY2UgY29kZVxuXHQgICAgbi0tO1xuXHQgICAgYnw9KHoubmV4dF9pbltwKytdJjB4ZmYpPDxrO2srPTg7XG5cdCAgfVxuXG5cdCAgdD0gYiZtZDtcblx0ICB0cD10ZDtcblx0ICB0cF9pbmRleD10ZF9pbmRleDtcbiAgICAgICAgICB0cF9pbmRleF90XzM9KHRwX2luZGV4K3QpKjM7XG5cdCAgZSA9IHRwW3RwX2luZGV4X3RfM107XG5cblx0ICBkbyB7XG5cblx0ICAgIGI+Pj0odHBbdHBfaW5kZXhfdF8zKzFdKTsgay09KHRwW3RwX2luZGV4X3RfMysxXSk7XG5cblx0ICAgIGlmKChlJjE2KSE9MCl7XG5cdCAgICAgIC8vIGdldCBleHRyYSBiaXRzIHRvIGFkZCB0byBkaXN0YW5jZSBiYXNlXG5cdCAgICAgIGUgJj0gMTU7XG5cdCAgICAgIHdoaWxlKGs8KGUpKXsgICAgICAgICAvLyBnZXQgZXh0cmEgYml0cyAodXAgdG8gMTMpXG5cdFx0bi0tO1xuXHRcdGJ8PSh6Lm5leHRfaW5bcCsrXSYweGZmKTw8aztrKz04O1xuXHQgICAgICB9XG5cblx0ICAgICAgZCA9IHRwW3RwX2luZGV4X3RfMysyXSArIChiJmluZmxhdGVfbWFza1tlXSk7XG5cblx0ICAgICAgYj4+PShlKTsgay09KGUpO1xuXG5cdCAgICAgIC8vIGRvIHRoZSBjb3B5XG5cdCAgICAgIG0gLT0gYztcblx0ICAgICAgaWYgKHEgPj0gZCl7ICAgICAgICAgICAgICAgIC8vIG9mZnNldCBiZWZvcmUgZGVzdFxuXHRcdC8vICBqdXN0IGNvcHlcblx0XHRyPXEtZDtcblx0XHRpZihxLXI+MCAmJiAyPihxLXIpKXsgICAgICAgICAgIFxuXHRcdCAgcy53aW5kb3dbcSsrXT1zLndpbmRvd1tyKytdOyAvLyBtaW5pbXVtIGNvdW50IGlzIHRocmVlLFxuXHRcdCAgcy53aW5kb3dbcSsrXT1zLndpbmRvd1tyKytdOyAvLyBzbyB1bnJvbGwgbG9vcCBhIGxpdHRsZVxuXHRcdCAgYy09Mjtcblx0XHR9XG5cdFx0ZWxzZXtcblx0XHQgIHMud2luZG93W3ErK109cy53aW5kb3dbcisrXTsgLy8gbWluaW11bSBjb3VudCBpcyB0aHJlZSxcblx0XHQgIHMud2luZG93W3ErK109cy53aW5kb3dbcisrXTsgLy8gc28gdW5yb2xsIGxvb3AgYSBsaXR0bGVcblx0XHQgIGMtPTI7XG5cdFx0fVxuXHQgICAgICB9XG5cdCAgICAgIGVsc2V7ICAgICAgICAgICAgICAgICAgLy8gZWxzZSBvZmZzZXQgYWZ0ZXIgZGVzdGluYXRpb25cbiAgICAgICAgICAgICAgICByPXEtZDtcbiAgICAgICAgICAgICAgICBkb3tcbiAgICAgICAgICAgICAgICAgIHIrPXMuZW5kOyAgICAgICAgICAvLyBmb3JjZSBwb2ludGVyIGluIHdpbmRvd1xuICAgICAgICAgICAgICAgIH13aGlsZShyPDApOyAgICAgICAgIC8vIGNvdmVycyBpbnZhbGlkIGRpc3RhbmNlc1xuXHRcdGU9cy5lbmQtcjtcblx0XHRpZihjPmUpeyAgICAgICAgICAgICAvLyBpZiBzb3VyY2UgY3Jvc3Nlcyxcblx0XHQgIGMtPWU7ICAgICAgICAgICAgICAvLyB3cmFwcGVkIGNvcHlcblx0XHQgIGlmKHEtcj4wICYmIGU+KHEtcikpeyAgICAgICAgICAgXG5cdFx0ICAgIGRve3Mud2luZG93W3ErK10gPSBzLndpbmRvd1tyKytdO31cblx0XHQgICAgd2hpbGUoLS1lIT0wKTtcblx0XHQgIH1cblx0XHQgIGVsc2V7XG5cdFx0ICAgIGFycmF5Q29weShzLndpbmRvdywgciwgcy53aW5kb3csIHEsIGUpO1xuXHRcdCAgICBxKz1lOyByKz1lOyBlPTA7XG5cdFx0ICB9XG5cdFx0ICByID0gMDsgICAgICAgICAgICAgICAgICAvLyBjb3B5IHJlc3QgZnJvbSBzdGFydCBvZiB3aW5kb3dcblx0XHR9XG5cblx0ICAgICAgfVxuXG5cdCAgICAgIC8vIGNvcHkgYWxsIG9yIHdoYXQncyBsZWZ0XG4gICAgICAgICAgICAgIGRve3Mud2luZG93W3ErK10gPSBzLndpbmRvd1tyKytdO31cblx0XHR3aGlsZSgtLWMhPTApO1xuXHQgICAgICBicmVhaztcblx0ICAgIH1cblx0ICAgIGVsc2UgaWYoKGUmNjQpPT0wKXtcblx0ICAgICAgdCs9dHBbdHBfaW5kZXhfdF8zKzJdO1xuXHQgICAgICB0Kz0oYiZpbmZsYXRlX21hc2tbZV0pO1xuXHQgICAgICB0cF9pbmRleF90XzM9KHRwX2luZGV4K3QpKjM7XG5cdCAgICAgIGU9dHBbdHBfaW5kZXhfdF8zXTtcblx0ICAgIH1cblx0ICAgIGVsc2V7XG5cdCAgICAgIHoubXNnID0gXCJpbnZhbGlkIGRpc3RhbmNlIGNvZGVcIjtcblxuXHQgICAgICBjPXouYXZhaWxfaW4tbjtjPShrPj4zKTxjP2s+PjM6YztuKz1jO3AtPWM7ay09Yzw8MztcblxuXHQgICAgICBzLmJpdGI9YjtzLmJpdGs9aztcblx0ICAgICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgICAgICBzLndyaXRlPXE7XG5cblx0ICAgICAgcmV0dXJuIFpfREFUQV9FUlJPUjtcblx0ICAgIH1cblx0ICB9XG5cdCAgd2hpbGUodHJ1ZSk7XG5cdCAgYnJlYWs7XG5cdH1cblxuXHRpZigoZSY2NCk9PTApe1xuXHQgIHQrPXRwW3RwX2luZGV4X3RfMysyXTtcblx0ICB0Kz0oYiZpbmZsYXRlX21hc2tbZV0pO1xuXHQgIHRwX2luZGV4X3RfMz0odHBfaW5kZXgrdCkqMztcblx0ICBpZigoZT10cFt0cF9pbmRleF90XzNdKT09MCl7XG5cblx0ICAgIGI+Pj0odHBbdHBfaW5kZXhfdF8zKzFdKTsgay09KHRwW3RwX2luZGV4X3RfMysxXSk7XG5cblx0ICAgIHMud2luZG93W3ErK109dHBbdHBfaW5kZXhfdF8zKzJdO1xuXHQgICAgbS0tO1xuXHQgICAgYnJlYWs7XG5cdCAgfVxuXHR9XG5cdGVsc2UgaWYoKGUmMzIpIT0wKXtcblxuXHQgIGM9ei5hdmFpbF9pbi1uO2M9KGs+PjMpPGM/az4+MzpjO24rPWM7cC09YztrLT1jPDwzO1xuIFxuXHQgIHMuYml0Yj1iO3MuYml0az1rO1xuXHQgIHouYXZhaWxfaW49bjt6LnRvdGFsX2luKz1wLXoubmV4dF9pbl9pbmRleDt6Lm5leHRfaW5faW5kZXg9cDtcblx0ICBzLndyaXRlPXE7XG5cblx0ICByZXR1cm4gWl9TVFJFQU1fRU5EO1xuXHR9XG5cdGVsc2V7XG5cdCAgei5tc2c9XCJpbnZhbGlkIGxpdGVyYWwvbGVuZ3RoIGNvZGVcIjtcblxuXHQgIGM9ei5hdmFpbF9pbi1uO2M9KGs+PjMpPGM/az4+MzpjO24rPWM7cC09YztrLT1jPDwzO1xuXG5cdCAgcy5iaXRiPWI7cy5iaXRrPWs7XG5cdCAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuXHQgIHMud3JpdGU9cTtcblxuXHQgIHJldHVybiBaX0RBVEFfRVJST1I7XG5cdH1cbiAgICAgIH0gXG4gICAgICB3aGlsZSh0cnVlKTtcbiAgICB9IFxuICAgIHdoaWxlKG0+PTI1OCAmJiBuPj0gMTApO1xuXG4gICAgLy8gbm90IGVub3VnaCBpbnB1dCBvciBvdXRwdXQtLXJlc3RvcmUgcG9pbnRlcnMgYW5kIHJldHVyblxuICAgIGM9ei5hdmFpbF9pbi1uO2M9KGs+PjMpPGM/az4+MzpjO24rPWM7cC09YztrLT1jPDwzO1xuXG4gICAgcy5iaXRiPWI7cy5iaXRrPWs7XG4gICAgei5hdmFpbF9pbj1uO3oudG90YWxfaW4rPXAtei5uZXh0X2luX2luZGV4O3oubmV4dF9pbl9pbmRleD1wO1xuICAgIHMud3JpdGU9cTtcblxuICAgIHJldHVybiBaX09LO1xufVxuXG4vL1xuLy8gSW5mVHJlZS5qYXZhXG4vL1xuXG5mdW5jdGlvbiBJbmZUcmVlKCkge1xufVxuXG5JbmZUcmVlLnByb3RvdHlwZS5odWZ0X2J1aWxkID0gZnVuY3Rpb24oYiwgYmluZGV4LCBuLCBzLCBkLCBlLCB0LCBtLCBocCwgaG4sIHYpIHtcblxuICAgIC8vIEdpdmVuIGEgbGlzdCBvZiBjb2RlIGxlbmd0aHMgYW5kIGEgbWF4aW11bSB0YWJsZSBzaXplLCBtYWtlIGEgc2V0IG9mXG4gICAgLy8gdGFibGVzIHRvIGRlY29kZSB0aGF0IHNldCBvZiBjb2Rlcy4gIFJldHVybiBaX09LIG9uIHN1Y2Nlc3MsIFpfQlVGX0VSUk9SXG4gICAgLy8gaWYgdGhlIGdpdmVuIGNvZGUgc2V0IGlzIGluY29tcGxldGUgKHRoZSB0YWJsZXMgYXJlIHN0aWxsIGJ1aWx0IGluIHRoaXNcbiAgICAvLyBjYXNlKSwgWl9EQVRBX0VSUk9SIGlmIHRoZSBpbnB1dCBpcyBpbnZhbGlkIChhbiBvdmVyLXN1YnNjcmliZWQgc2V0IG9mXG4gICAgLy8gbGVuZ3RocyksIG9yIFpfTUVNX0VSUk9SIGlmIG5vdCBlbm91Z2ggbWVtb3J5LlxuXG4gICAgdmFyIGE7ICAgICAgICAgICAgICAgICAgICAgICAvLyBjb3VudGVyIGZvciBjb2RlcyBvZiBsZW5ndGgga1xuICAgIHZhciBmOyAgICAgICAgICAgICAgICAgICAgICAgLy8gaSByZXBlYXRzIGluIHRhYmxlIGV2ZXJ5IGYgZW50cmllc1xuICAgIHZhciBnOyAgICAgICAgICAgICAgICAgICAgICAgLy8gbWF4aW11bSBjb2RlIGxlbmd0aFxuICAgIHZhciBoOyAgICAgICAgICAgICAgICAgICAgICAgLy8gdGFibGUgbGV2ZWxcbiAgICB2YXIgaTsgICAgICAgICAgICAgICAgICAgICAgIC8vIGNvdW50ZXIsIGN1cnJlbnQgY29kZVxuICAgIHZhciBqOyAgICAgICAgICAgICAgICAgICAgICAgLy8gY291bnRlclxuICAgIHZhciBrOyAgICAgICAgICAgICAgICAgICAgICAgLy8gbnVtYmVyIG9mIGJpdHMgaW4gY3VycmVudCBjb2RlXG4gICAgdmFyIGw7ICAgICAgICAgICAgICAgICAgICAgICAvLyBiaXRzIHBlciB0YWJsZSAocmV0dXJuZWQgaW4gbSlcbiAgICB2YXIgbWFzazsgICAgICAgICAgICAgICAgICAgIC8vICgxIDw8IHcpIC0gMSwgdG8gYXZvaWQgY2MgLU8gYnVnIG9uIEhQXG4gICAgdmFyIHA7ICAgICAgICAgICAgICAgICAgICAgICAvLyBwb2ludGVyIGludG8gY1tdLCBiW10sIG9yIHZbXVxuICAgIHZhciBxOyAgICAgICAgICAgICAgICAgICAgICAgLy8gcG9pbnRzIHRvIGN1cnJlbnQgdGFibGVcbiAgICB2YXIgdzsgICAgICAgICAgICAgICAgICAgICAgIC8vIGJpdHMgYmVmb3JlIHRoaXMgdGFibGUgPT0gKGwgKiBoKVxuICAgIHZhciB4cDsgICAgICAgICAgICAgICAgICAgICAgLy8gcG9pbnRlciBpbnRvIHhcbiAgICB2YXIgeTsgICAgICAgICAgICAgICAgICAgICAgIC8vIG51bWJlciBvZiBkdW1teSBjb2RlcyBhZGRlZFxuICAgIHZhciB6OyAgICAgICAgICAgICAgICAgICAgICAgLy8gbnVtYmVyIG9mIGVudHJpZXMgaW4gY3VycmVudCB0YWJsZVxuXG4gICAgLy8gR2VuZXJhdGUgY291bnRzIGZvciBlYWNoIGJpdCBsZW5ndGhcblxuICAgIHAgPSAwOyBpID0gbjtcbiAgICBkbyB7XG4gICAgICB0aGlzLmNbYltiaW5kZXgrcF1dKys7IHArKzsgaS0tOyAgIC8vIGFzc3VtZSBhbGwgZW50cmllcyA8PSBCTUFYXG4gICAgfXdoaWxlKGkhPTApO1xuXG4gICAgaWYodGhpcy5jWzBdID09IG4peyAgICAgICAgICAgICAgICAvLyBudWxsIGlucHV0LS1hbGwgemVybyBsZW5ndGggY29kZXNcbiAgICAgIHRbMF0gPSAtMTtcbiAgICAgIG1bMF0gPSAwO1xuICAgICAgcmV0dXJuIFpfT0s7XG4gICAgfVxuXG4gICAgLy8gRmluZCBtaW5pbXVtIGFuZCBtYXhpbXVtIGxlbmd0aCwgYm91bmQgKm0gYnkgdGhvc2VcbiAgICBsID0gbVswXTtcbiAgICBmb3IgKGogPSAxOyBqIDw9IEJNQVg7IGorKylcbiAgICAgIGlmKHRoaXMuY1tqXSE9MCkgYnJlYWs7XG4gICAgayA9IGo7ICAgICAgICAgICAgICAgICAgICAgICAgLy8gbWluaW11bSBjb2RlIGxlbmd0aFxuICAgIGlmKGwgPCBqKXtcbiAgICAgIGwgPSBqO1xuICAgIH1cbiAgICBmb3IgKGkgPSBCTUFYOyBpIT0wOyBpLS0pe1xuICAgICAgaWYodGhpcy5jW2ldIT0wKSBicmVhaztcbiAgICB9XG4gICAgZyA9IGk7ICAgICAgICAgICAgICAgICAgICAgICAgLy8gbWF4aW11bSBjb2RlIGxlbmd0aFxuICAgIGlmKGwgPiBpKXtcbiAgICAgIGwgPSBpO1xuICAgIH1cbiAgICBtWzBdID0gbDtcblxuICAgIC8vIEFkanVzdCBsYXN0IGxlbmd0aCBjb3VudCB0byBmaWxsIG91dCBjb2RlcywgaWYgbmVlZGVkXG4gICAgZm9yICh5ID0gMSA8PCBqOyBqIDwgaTsgaisrLCB5IDw8PSAxKXtcbiAgICAgIGlmICgoeSAtPSB0aGlzLmNbal0pIDwgMCl7XG4gICAgICAgIHJldHVybiBaX0RBVEFfRVJST1I7XG4gICAgICB9XG4gICAgfVxuICAgIGlmICgoeSAtPSB0aGlzLmNbaV0pIDwgMCl7XG4gICAgICByZXR1cm4gWl9EQVRBX0VSUk9SO1xuICAgIH1cbiAgICB0aGlzLmNbaV0gKz0geTtcblxuICAgIC8vIEdlbmVyYXRlIHN0YXJ0aW5nIG9mZnNldHMgaW50byB0aGUgdmFsdWUgdGFibGUgZm9yIGVhY2ggbGVuZ3RoXG4gICAgdGhpcy54WzFdID0gaiA9IDA7XG4gICAgcCA9IDE7ICB4cCA9IDI7XG4gICAgd2hpbGUgKC0taSE9MCkgeyAgICAgICAgICAgICAgICAgLy8gbm90ZSB0aGF0IGkgPT0gZyBmcm9tIGFib3ZlXG4gICAgICB0aGlzLnhbeHBdID0gKGogKz0gdGhpcy5jW3BdKTtcbiAgICAgIHhwKys7XG4gICAgICBwKys7XG4gICAgfVxuXG4gICAgLy8gTWFrZSBhIHRhYmxlIG9mIHZhbHVlcyBpbiBvcmRlciBvZiBiaXQgbGVuZ3Roc1xuICAgIGkgPSAwOyBwID0gMDtcbiAgICBkbyB7XG4gICAgICBpZiAoKGogPSBiW2JpbmRleCtwXSkgIT0gMCl7XG4gICAgICAgIHRoaXMudlt0aGlzLnhbal0rK10gPSBpO1xuICAgICAgfVxuICAgICAgcCsrO1xuICAgIH1cbiAgICB3aGlsZSAoKytpIDwgbik7XG4gICAgbiA9IHRoaXMueFtnXTsgICAgICAgICAgICAgICAgICAgICAvLyBzZXQgbiB0byBsZW5ndGggb2YgdlxuXG4gICAgLy8gR2VuZXJhdGUgdGhlIEh1ZmZtYW4gY29kZXMgYW5kIGZvciBlYWNoLCBtYWtlIHRoZSB0YWJsZSBlbnRyaWVzXG4gICAgdGhpcy54WzBdID0gaSA9IDA7ICAgICAgICAgICAgICAgICAvLyBmaXJzdCBIdWZmbWFuIGNvZGUgaXMgemVyb1xuICAgIHAgPSAwOyAgICAgICAgICAgICAgICAgICAgICAgIC8vIGdyYWIgdmFsdWVzIGluIGJpdCBvcmRlclxuICAgIGggPSAtMTsgICAgICAgICAgICAgICAgICAgICAgIC8vIG5vIHRhYmxlcyB5ZXQtLWxldmVsIC0xXG4gICAgdyA9IC1sOyAgICAgICAgICAgICAgICAgICAgICAgLy8gYml0cyBkZWNvZGVkID09IChsICogaClcbiAgICB0aGlzLnVbMF0gPSAwOyAgICAgICAgICAgICAgICAgICAgIC8vIGp1c3QgdG8ga2VlcCBjb21waWxlcnMgaGFwcHlcbiAgICBxID0gMDsgICAgICAgICAgICAgICAgICAgICAgICAvLyBkaXR0b1xuICAgIHogPSAwOyAgICAgICAgICAgICAgICAgICAgICAgIC8vIGRpdHRvXG5cbiAgICAvLyBnbyB0aHJvdWdoIHRoZSBiaXQgbGVuZ3RocyAoayBhbHJlYWR5IGlzIGJpdHMgaW4gc2hvcnRlc3QgY29kZSlcbiAgICBmb3IgKDsgayA8PSBnOyBrKyspe1xuICAgICAgYSA9IHRoaXMuY1trXTtcbiAgICAgIHdoaWxlIChhLS0hPTApe1xuXHQvLyBoZXJlIGkgaXMgdGhlIEh1ZmZtYW4gY29kZSBvZiBsZW5ndGggayBiaXRzIGZvciB2YWx1ZSAqcFxuXHQvLyBtYWtlIHRhYmxlcyB1cCB0byByZXF1aXJlZCBsZXZlbFxuICAgICAgICB3aGlsZSAoayA+IHcgKyBsKXtcbiAgICAgICAgICBoKys7XG4gICAgICAgICAgdyArPSBsOyAgICAgICAgICAgICAgICAgLy8gcHJldmlvdXMgdGFibGUgYWx3YXlzIGwgYml0c1xuXHQgIC8vIGNvbXB1dGUgbWluaW11bSBzaXplIHRhYmxlIGxlc3MgdGhhbiBvciBlcXVhbCB0byBsIGJpdHNcbiAgICAgICAgICB6ID0gZyAtIHc7XG4gICAgICAgICAgeiA9ICh6ID4gbCkgPyBsIDogejsgICAgICAgIC8vIHRhYmxlIHNpemUgdXBwZXIgbGltaXRcbiAgICAgICAgICBpZigoZj0xPDwoaj1rLXcpKT5hKzEpeyAgICAgLy8gdHJ5IGEgay13IGJpdCB0YWJsZVxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAvLyB0b28gZmV3IGNvZGVzIGZvciBrLXcgYml0IHRhYmxlXG4gICAgICAgICAgICBmIC09IGEgKyAxOyAgICAgICAgICAgICAgIC8vIGRlZHVjdCBjb2RlcyBmcm9tIHBhdHRlcm5zIGxlZnRcbiAgICAgICAgICAgIHhwID0gaztcbiAgICAgICAgICAgIGlmKGogPCB6KXtcbiAgICAgICAgICAgICAgd2hpbGUgKCsraiA8IHopeyAgICAgICAgLy8gdHJ5IHNtYWxsZXIgdGFibGVzIHVwIHRvIHogYml0c1xuICAgICAgICAgICAgICAgIGlmKChmIDw8PSAxKSA8PSB0aGlzLmNbKyt4cF0pXG4gICAgICAgICAgICAgICAgICBicmVhazsgICAgICAgICAgICAgIC8vIGVub3VnaCBjb2RlcyB0byB1c2UgdXAgaiBiaXRzXG4gICAgICAgICAgICAgICAgZiAtPSB0aGlzLmNbeHBdOyAgICAgICAgICAgLy8gZWxzZSBkZWR1Y3QgY29kZXMgZnJvbSBwYXR0ZXJuc1xuICAgICAgICAgICAgICB9XG5cdCAgICB9XG4gICAgICAgICAgfVxuICAgICAgICAgIHogPSAxIDw8IGo7ICAgICAgICAgICAgICAgICAvLyB0YWJsZSBlbnRyaWVzIGZvciBqLWJpdCB0YWJsZVxuXG5cdCAgLy8gYWxsb2NhdGUgbmV3IHRhYmxlXG4gICAgICAgICAgaWYgKHRoaXMuaG5bMF0gKyB6ID4gTUFOWSl7ICAgICAgIC8vIChub3RlOiBkb2Vzbid0IG1hdHRlciBmb3IgZml4ZWQpXG4gICAgICAgICAgICByZXR1cm4gWl9EQVRBX0VSUk9SOyAgICAgICAvLyBvdmVyZmxvdyBvZiBNQU5ZXG4gICAgICAgICAgfVxuICAgICAgICAgIHRoaXMudVtoXSA9IHEgPSAvKmhwKyovIHRoaXMuaG5bMF07ICAgLy8gREVCVUdcbiAgICAgICAgICB0aGlzLmhuWzBdICs9IHo7XG4gXG5cdCAgLy8gY29ubmVjdCB0byBsYXN0IHRhYmxlLCBpZiB0aGVyZSBpcyBvbmVcblx0ICBpZihoIT0wKXtcbiAgICAgICAgICAgIHRoaXMueFtoXT1pOyAgICAgICAgICAgLy8gc2F2ZSBwYXR0ZXJuIGZvciBiYWNraW5nIHVwXG4gICAgICAgICAgICB0aGlzLnJbMF09ajsgICAgIC8vIGJpdHMgaW4gdGhpcyB0YWJsZVxuICAgICAgICAgICAgdGhpcy5yWzFdPWw7ICAgICAvLyBiaXRzIHRvIGR1bXAgYmVmb3JlIHRoaXMgdGFibGVcbiAgICAgICAgICAgIGo9aT4+Pih3IC0gbCk7XG4gICAgICAgICAgICB0aGlzLnJbMl0gPSAocSAtIHRoaXMudVtoLTFdIC0gaik7ICAgICAgICAgICAgICAgLy8gb2Zmc2V0IHRvIHRoaXMgdGFibGVcbiAgICAgICAgICAgIGFycmF5Q29weSh0aGlzLnIsIDAsIGhwLCAodGhpcy51W2gtMV0raikqMywgMyk7IC8vIGNvbm5lY3QgdG8gbGFzdCB0YWJsZVxuICAgICAgICAgIH1cbiAgICAgICAgICBlbHNle1xuICAgICAgICAgICAgdFswXSA9IHE7ICAgICAgICAgICAgICAgLy8gZmlyc3QgdGFibGUgaXMgcmV0dXJuZWQgcmVzdWx0XG5cdCAgfVxuICAgICAgICB9XG5cblx0Ly8gc2V0IHVwIHRhYmxlIGVudHJ5IGluIHJcbiAgICAgICAgdGhpcy5yWzFdID0gKGsgLSB3KTtcbiAgICAgICAgaWYgKHAgPj0gbil7XG4gICAgICAgICAgdGhpcy5yWzBdID0gMTI4ICsgNjQ7ICAgICAgLy8gb3V0IG9mIHZhbHVlcy0taW52YWxpZCBjb2RlXG5cdH1cbiAgICAgICAgZWxzZSBpZiAodltwXSA8IHMpe1xuICAgICAgICAgIHRoaXMuclswXSA9ICh0aGlzLnZbcF0gPCAyNTYgPyAwIDogMzIgKyA2NCk7ICAvLyAyNTYgaXMgZW5kLW9mLWJsb2NrXG4gICAgICAgICAgdGhpcy5yWzJdID0gdGhpcy52W3ArK107ICAgICAgICAgIC8vIHNpbXBsZSBjb2RlIGlzIGp1c3QgdGhlIHZhbHVlXG4gICAgICAgIH1cbiAgICAgICAgZWxzZXtcbiAgICAgICAgICB0aGlzLnJbMF09KGVbdGhpcy52W3BdLXNdKzE2KzY0KTsgLy8gbm9uLXNpbXBsZS0tbG9vayB1cCBpbiBsaXN0c1xuICAgICAgICAgIHRoaXMuclsyXT1kW3RoaXMudltwKytdIC0gc107XG4gICAgICAgIH1cblxuICAgICAgICAvLyBmaWxsIGNvZGUtbGlrZSBlbnRyaWVzIHdpdGggclxuICAgICAgICBmPTE8PChrLXcpO1xuICAgICAgICBmb3IgKGo9aT4+Pnc7ajx6O2orPWYpe1xuICAgICAgICAgIGFycmF5Q29weSh0aGlzLnIsIDAsIGhwLCAocStqKSozLCAzKTtcblx0fVxuXG5cdC8vIGJhY2t3YXJkcyBpbmNyZW1lbnQgdGhlIGstYml0IGNvZGUgaVxuICAgICAgICBmb3IgKGogPSAxIDw8IChrIC0gMSk7IChpICYgaikhPTA7IGogPj4+PSAxKXtcbiAgICAgICAgICBpIF49IGo7XG5cdH1cbiAgICAgICAgaSBePSBqO1xuXG5cdC8vIGJhY2t1cCBvdmVyIGZpbmlzaGVkIHRhYmxlc1xuICAgICAgICBtYXNrID0gKDEgPDwgdykgLSAxOyAgICAgIC8vIG5lZWRlZCBvbiBIUCwgY2MgLU8gYnVnXG4gICAgICAgIHdoaWxlICgoaSAmIG1hc2spICE9IHRoaXMueFtoXSl7XG4gICAgICAgICAgaC0tOyAgICAgICAgICAgICAgICAgICAgLy8gZG9uJ3QgbmVlZCB0byB1cGRhdGUgcVxuICAgICAgICAgIHcgLT0gbDtcbiAgICAgICAgICBtYXNrID0gKDEgPDwgdykgLSAxO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuICAgIC8vIFJldHVybiBaX0JVRl9FUlJPUiBpZiB3ZSB3ZXJlIGdpdmVuIGFuIGluY29tcGxldGUgdGFibGVcbiAgICByZXR1cm4geSAhPSAwICYmIGcgIT0gMSA/IFpfQlVGX0VSUk9SIDogWl9PSztcbn1cblxuSW5mVHJlZS5wcm90b3R5cGUuaW5mbGF0ZV90cmVlc19iaXRzID0gZnVuY3Rpb24oYywgYmIsIHRiLCBocCwgeikge1xuICAgIHZhciByZXN1bHQ7XG4gICAgdGhpcy5pbml0V29ya0FyZWEoMTkpO1xuICAgIHRoaXMuaG5bMF09MDtcbiAgICByZXN1bHQgPSB0aGlzLmh1ZnRfYnVpbGQoYywgMCwgMTksIDE5LCBudWxsLCBudWxsLCB0YiwgYmIsIGhwLCB0aGlzLmhuLCB0aGlzLnYpO1xuXG4gICAgaWYocmVzdWx0ID09IFpfREFUQV9FUlJPUil7XG4gICAgICB6Lm1zZyA9IFwib3ZlcnN1YnNjcmliZWQgZHluYW1pYyBiaXQgbGVuZ3RocyB0cmVlXCI7XG4gICAgfVxuICAgIGVsc2UgaWYocmVzdWx0ID09IFpfQlVGX0VSUk9SIHx8IGJiWzBdID09IDApe1xuICAgICAgei5tc2cgPSBcImluY29tcGxldGUgZHluYW1pYyBiaXQgbGVuZ3RocyB0cmVlXCI7XG4gICAgICByZXN1bHQgPSBaX0RBVEFfRVJST1I7XG4gICAgfVxuICAgIHJldHVybiByZXN1bHQ7XG59XG5cbkluZlRyZWUucHJvdG90eXBlLmluZmxhdGVfdHJlZXNfZHluYW1pYyA9IGZ1bmN0aW9uKG5sLCBuZCwgYywgYmwsIGJkLCB0bCwgdGQsIGhwLCB6KSB7XG4gICAgdmFyIHJlc3VsdDtcblxuICAgIC8vIGJ1aWxkIGxpdGVyYWwvbGVuZ3RoIHRyZWVcbiAgICB0aGlzLmluaXRXb3JrQXJlYSgyODgpO1xuICAgIHRoaXMuaG5bMF09MDtcbiAgICByZXN1bHQgPSB0aGlzLmh1ZnRfYnVpbGQoYywgMCwgbmwsIDI1NywgY3BsZW5zLCBjcGxleHQsIHRsLCBibCwgaHAsIHRoaXMuaG4sIHRoaXMudik7XG4gICAgaWYgKHJlc3VsdCAhPSBaX09LIHx8IGJsWzBdID09IDApe1xuICAgICAgaWYocmVzdWx0ID09IFpfREFUQV9FUlJPUil7XG4gICAgICAgIHoubXNnID0gXCJvdmVyc3Vic2NyaWJlZCBsaXRlcmFsL2xlbmd0aCB0cmVlXCI7XG4gICAgICB9XG4gICAgICBlbHNlIGlmIChyZXN1bHQgIT0gWl9NRU1fRVJST1Ipe1xuICAgICAgICB6Lm1zZyA9IFwiaW5jb21wbGV0ZSBsaXRlcmFsL2xlbmd0aCB0cmVlXCI7XG4gICAgICAgIHJlc3VsdCA9IFpfREFUQV9FUlJPUjtcbiAgICAgIH1cbiAgICAgIHJldHVybiByZXN1bHQ7XG4gICAgfVxuXG4gICAgLy8gYnVpbGQgZGlzdGFuY2UgdHJlZVxuICAgIHRoaXMuaW5pdFdvcmtBcmVhKDI4OCk7XG4gICAgcmVzdWx0ID0gdGhpcy5odWZ0X2J1aWxkKGMsIG5sLCBuZCwgMCwgY3BkaXN0LCBjcGRleHQsIHRkLCBiZCwgaHAsIHRoaXMuaG4sIHRoaXMudik7XG5cbiAgICBpZiAocmVzdWx0ICE9IFpfT0sgfHwgKGJkWzBdID09IDAgJiYgbmwgPiAyNTcpKXtcbiAgICAgIGlmIChyZXN1bHQgPT0gWl9EQVRBX0VSUk9SKXtcbiAgICAgICAgei5tc2cgPSBcIm92ZXJzdWJzY3JpYmVkIGRpc3RhbmNlIHRyZWVcIjtcbiAgICAgIH1cbiAgICAgIGVsc2UgaWYgKHJlc3VsdCA9PSBaX0JVRl9FUlJPUikge1xuICAgICAgICB6Lm1zZyA9IFwiaW5jb21wbGV0ZSBkaXN0YW5jZSB0cmVlXCI7XG4gICAgICAgIHJlc3VsdCA9IFpfREFUQV9FUlJPUjtcbiAgICAgIH1cbiAgICAgIGVsc2UgaWYgKHJlc3VsdCAhPSBaX01FTV9FUlJPUil7XG4gICAgICAgIHoubXNnID0gXCJlbXB0eSBkaXN0YW5jZSB0cmVlIHdpdGggbGVuZ3Roc1wiO1xuICAgICAgICByZXN1bHQgPSBaX0RBVEFfRVJST1I7XG4gICAgICB9XG4gICAgICByZXR1cm4gcmVzdWx0O1xuICAgIH1cblxuICAgIHJldHVybiBaX09LO1xufVxuLypcbiAgc3RhdGljIGludCBpbmZsYXRlX3RyZWVzX2ZpeGVkKGludFtdIGJsLCAgLy9saXRlcmFsIGRlc2lyZWQvYWN0dWFsIGJpdCBkZXB0aFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW50W10gYmQsICAvL2Rpc3RhbmNlIGRlc2lyZWQvYWN0dWFsIGJpdCBkZXB0aFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgaW50W11bXSB0bCwvL2xpdGVyYWwvbGVuZ3RoIHRyZWUgcmVzdWx0XG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBpbnRbXVtdIHRkLC8vZGlzdGFuY2UgdHJlZSByZXN1bHQgXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICBaU3RyZWFtIHogIC8vZm9yIG1lbW9yeSBhbGxvY2F0aW9uXG5cdFx0XHRcdCApe1xuXG4qL1xuXG5mdW5jdGlvbiBpbmZsYXRlX3RyZWVzX2ZpeGVkKGJsLCBiZCwgdGwsIHRkLCB6KSB7XG4gICAgYmxbMF09Zml4ZWRfYmw7XG4gICAgYmRbMF09Zml4ZWRfYmQ7XG4gICAgdGxbMF09Zml4ZWRfdGw7XG4gICAgdGRbMF09Zml4ZWRfdGQ7XG4gICAgcmV0dXJuIFpfT0s7XG59XG5cbkluZlRyZWUucHJvdG90eXBlLmluaXRXb3JrQXJlYSA9IGZ1bmN0aW9uKHZzaXplKXtcbiAgICBpZih0aGlzLmhuPT1udWxsKXtcbiAgICAgICAgdGhpcy5obj1uZXcgSW50MzJBcnJheSgxKTtcbiAgICAgICAgdGhpcy52PW5ldyBJbnQzMkFycmF5KHZzaXplKTtcbiAgICAgICAgdGhpcy5jPW5ldyBJbnQzMkFycmF5KEJNQVgrMSk7XG4gICAgICAgIHRoaXMucj1uZXcgSW50MzJBcnJheSgzKTtcbiAgICAgICAgdGhpcy51PW5ldyBJbnQzMkFycmF5KEJNQVgpO1xuICAgICAgICB0aGlzLng9bmV3IEludDMyQXJyYXkoQk1BWCsxKTtcbiAgICB9XG4gICAgaWYodGhpcy52Lmxlbmd0aDx2c2l6ZSl7IFxuICAgICAgICB0aGlzLnY9bmV3IEludDMyQXJyYXkodnNpemUpOyBcbiAgICB9XG4gICAgZm9yKHZhciBpPTA7IGk8dnNpemU7IGkrKyl7dGhpcy52W2ldPTA7fVxuICAgIGZvcih2YXIgaT0wOyBpPEJNQVgrMTsgaSsrKXt0aGlzLmNbaV09MDt9XG4gICAgZm9yKHZhciBpPTA7IGk8MzsgaSsrKXt0aGlzLnJbaV09MDt9XG4vLyAgZm9yKGludCBpPTA7IGk8Qk1BWDsgaSsrKXt1W2ldPTA7fVxuICAgIGFycmF5Q29weSh0aGlzLmMsIDAsIHRoaXMudSwgMCwgQk1BWCk7XG4vLyAgZm9yKGludCBpPTA7IGk8Qk1BWCsxOyBpKyspe3hbaV09MDt9XG4gICAgYXJyYXlDb3B5KHRoaXMuYywgMCwgdGhpcy54LCAwLCBCTUFYKzEpO1xufVxuXG52YXIgdGVzdEFycmF5ID0gbmV3IFVpbnQ4QXJyYXkoMSk7XG52YXIgaGFzU3ViYXJyYXkgPSAodHlwZW9mIHRlc3RBcnJheS5zdWJhcnJheSA9PT0gJ2Z1bmN0aW9uJyk7XG52YXIgaGFzU2xpY2UgPSBmYWxzZTsgLyogKHR5cGVvZiB0ZXN0QXJyYXkuc2xpY2UgPT09ICdmdW5jdGlvbicpOyAqLyAvLyBDaHJvbWUgc2xpY2UgcGVyZm9ybWFuY2UgaXMgc28gZGlyZSB0aGF0IHdlJ3JlIGN1cnJlbnRseSBub3QgdXNpbmcgaXQuLi5cblxuZnVuY3Rpb24gYXJyYXlDb3B5KHNyYywgc3JjT2Zmc2V0LCBkZXN0LCBkZXN0T2Zmc2V0LCBjb3VudCkge1xuICAgIGlmIChjb3VudCA9PSAwKSB7XG4gICAgICAgIHJldHVybjtcbiAgICB9IFxuICAgIGlmICghc3JjKSB7XG4gICAgICAgIHRocm93IFwiVW5kZWYgc3JjXCI7XG4gICAgfSBlbHNlIGlmICghZGVzdCkge1xuICAgICAgICB0aHJvdyBcIlVuZGVmIGRlc3RcIjtcbiAgICB9XG5cbiAgICBpZiAoc3JjT2Zmc2V0ID09IDAgJiYgY291bnQgPT0gc3JjLmxlbmd0aCkge1xuICAgICAgICBhcnJheUNvcHlfZmFzdChzcmMsIGRlc3QsIGRlc3RPZmZzZXQpO1xuICAgIH0gZWxzZSBpZiAoaGFzU3ViYXJyYXkpIHtcbiAgICAgICAgYXJyYXlDb3B5X2Zhc3Qoc3JjLnN1YmFycmF5KHNyY09mZnNldCwgc3JjT2Zmc2V0ICsgY291bnQpLCBkZXN0LCBkZXN0T2Zmc2V0KTsgXG4gICAgfSBlbHNlIGlmIChzcmMuQllURVNfUEVSX0VMRU1FTlQgPT0gMSAmJiBjb3VudCA+IDEwMCkge1xuICAgICAgICBhcnJheUNvcHlfZmFzdChuZXcgVWludDhBcnJheShzcmMuYnVmZmVyLCBzcmMuYnl0ZU9mZnNldCArIHNyY09mZnNldCwgY291bnQpLCBkZXN0LCBkZXN0T2Zmc2V0KTtcbiAgICB9IGVsc2UgeyBcbiAgICAgICAgYXJyYXlDb3B5X3Nsb3coc3JjLCBzcmNPZmZzZXQsIGRlc3QsIGRlc3RPZmZzZXQsIGNvdW50KTtcbiAgICB9XG5cbn1cblxuZnVuY3Rpb24gYXJyYXlDb3B5X3Nsb3coc3JjLCBzcmNPZmZzZXQsIGRlc3QsIGRlc3RPZmZzZXQsIGNvdW50KSB7XG5cbiAgICAvLyBkbG9nKCdfc2xvdyBjYWxsOiBzcmNPZmZzZXQ9JyArIHNyY09mZnNldCArICc7IGRlc3RPZmZzZXQ9JyArIGRlc3RPZmZzZXQgKyAnOyBjb3VudD0nICsgY291bnQpO1xuXG4gICAgIGZvciAodmFyIGkgPSAwOyBpIDwgY291bnQ7ICsraSkge1xuICAgICAgICBkZXN0W2Rlc3RPZmZzZXQgKyBpXSA9IHNyY1tzcmNPZmZzZXQgKyBpXTtcbiAgICB9XG59XG5cbmZ1bmN0aW9uIGFycmF5Q29weV9mYXN0KHNyYywgZGVzdCwgZGVzdE9mZnNldCkge1xuICAgIGRlc3Quc2V0KHNyYywgZGVzdE9mZnNldCk7XG59XG5cblxuICAvLyBsYXJnZXN0IHByaW1lIHNtYWxsZXIgdGhhbiA2NTUzNlxudmFyIEFETEVSX0JBU0U9NjU1MjE7IFxuICAvLyBOTUFYIGlzIHRoZSBsYXJnZXN0IG4gc3VjaCB0aGF0IDI1NW4obisxKS8yICsgKG4rMSkoQkFTRS0xKSA8PSAyXjMyLTFcbnZhciBBRExFUl9OTUFYPTU1NTI7XG5cbmZ1bmN0aW9uIGFkbGVyMzIoYWRsZXIsIC8qIGJ5dGVbXSAqLyBidWYsICBpbmRleCwgbGVuKXtcbiAgICBpZihidWYgPT0gbnVsbCl7IHJldHVybiAxOyB9XG5cbiAgICB2YXIgczE9YWRsZXImMHhmZmZmO1xuICAgIHZhciBzMj0oYWRsZXI+PjE2KSYweGZmZmY7XG4gICAgdmFyIGs7XG5cbiAgICB3aGlsZShsZW4gPiAwKSB7XG4gICAgICBrPWxlbjxBRExFUl9OTUFYP2xlbjpBRExFUl9OTUFYO1xuICAgICAgbGVuLT1rO1xuICAgICAgd2hpbGUoaz49MTYpe1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIHMxKz1idWZbaW5kZXgrK10mMHhmZjsgczIrPXMxO1xuICAgICAgICBzMSs9YnVmW2luZGV4KytdJjB4ZmY7IHMyKz1zMTtcbiAgICAgICAgay09MTY7XG4gICAgICB9XG4gICAgICBpZihrIT0wKXtcbiAgICAgICAgZG97XG4gICAgICAgICAgczErPWJ1ZltpbmRleCsrXSYweGZmOyBzMis9czE7XG4gICAgICAgIH1cbiAgICAgICAgd2hpbGUoLS1rIT0wKTtcbiAgICAgIH1cbiAgICAgIHMxJT1BRExFUl9CQVNFO1xuICAgICAgczIlPUFETEVSX0JBU0U7XG4gICAgfVxuICAgIHJldHVybiAoczI8PDE2KXxzMTtcbn1cblxuXG5cbmZ1bmN0aW9uIGpzemxpYl9pbmZsYXRlX2J1ZmZlcihidWZmZXIsIHN0YXJ0LCBsZW5ndGgsIGFmdGVyVW5jT2Zmc2V0KSB7XG4gICAgaWYgKCFzdGFydCkge1xuICAgICAgICBidWZmZXIgPSBuZXcgVWludDhBcnJheShidWZmZXIpO1xuICAgIH0gZWxzZSBpZiAoIWxlbmd0aCkge1xuICAgICAgICBidWZmZXIgPSBuZXcgVWludDhBcnJheShidWZmZXIsIHN0YXJ0LCBidWZmZXIuYnl0ZUxlbmd0aCAtIHN0YXJ0KTtcbiAgICB9IGVsc2Uge1xuICAgICAgICBidWZmZXIgPSBuZXcgVWludDhBcnJheShidWZmZXIsIHN0YXJ0LCBsZW5ndGgpO1xuICAgIH1cblxuICAgIHZhciB6ID0gbmV3IFpTdHJlYW0oKTtcbiAgICB6LmluZmxhdGVJbml0KERFRl9XQklUUywgdHJ1ZSk7XG4gICAgei5uZXh0X2luID0gYnVmZmVyO1xuICAgIHoubmV4dF9pbl9pbmRleCA9IDA7XG4gICAgei5hdmFpbF9pbiA9IGJ1ZmZlci5sZW5ndGg7XG5cbiAgICB2YXIgb0Jsb2NrTGlzdCA9IFtdO1xuICAgIHZhciB0b3RhbFNpemUgPSAwO1xuICAgIHdoaWxlICh0cnVlKSB7XG4gICAgICAgIHZhciBvYnVmID0gbmV3IFVpbnQ4QXJyYXkoMzIwMDApO1xuICAgICAgICB6Lm5leHRfb3V0ID0gb2J1ZjtcbiAgICAgICAgei5uZXh0X291dF9pbmRleCA9IDA7XG4gICAgICAgIHouYXZhaWxfb3V0ID0gb2J1Zi5sZW5ndGg7XG4gICAgICAgIHZhciBzdGF0dXMgPSB6LmluZmxhdGUoWl9OT19GTFVTSCk7XG4gICAgICAgIGlmIChzdGF0dXMgIT0gWl9PSyAmJiBzdGF0dXMgIT0gWl9TVFJFQU1fRU5EICYmIHN0YXR1cyAhPSBaX0JVRl9FUlJPUikge1xuICAgICAgICAgICAgdGhyb3cgei5tc2c7XG4gICAgICAgIH1cbiAgICAgICAgaWYgKHouYXZhaWxfb3V0ICE9IDApIHtcbiAgICAgICAgICAgIHZhciBuZXdvYiA9IG5ldyBVaW50OEFycmF5KG9idWYubGVuZ3RoIC0gei5hdmFpbF9vdXQpO1xuICAgICAgICAgICAgYXJyYXlDb3B5KG9idWYsIDAsIG5ld29iLCAwLCAob2J1Zi5sZW5ndGggLSB6LmF2YWlsX291dCkpO1xuICAgICAgICAgICAgb2J1ZiA9IG5ld29iO1xuICAgICAgICB9XG4gICAgICAgIG9CbG9ja0xpc3QucHVzaChvYnVmKTtcbiAgICAgICAgdG90YWxTaXplICs9IG9idWYubGVuZ3RoO1xuICAgICAgICBpZiAoc3RhdHVzID09IFpfU1RSRUFNX0VORCB8fCBzdGF0dXMgPT0gWl9CVUZfRVJST1IpIHtcbiAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICB9XG4gICAgfVxuXG4gICAgaWYgKGFmdGVyVW5jT2Zmc2V0KSB7XG4gICAgICAgIGFmdGVyVW5jT2Zmc2V0WzBdID0gKHN0YXJ0IHx8IDApICsgei5uZXh0X2luX2luZGV4O1xuICAgIH1cblxuICAgIGlmIChvQmxvY2tMaXN0Lmxlbmd0aCA9PSAxKSB7XG4gICAgICAgIHJldHVybiBvQmxvY2tMaXN0WzBdLmJ1ZmZlcjtcbiAgICB9IGVsc2Uge1xuICAgICAgICB2YXIgb3V0ID0gbmV3IFVpbnQ4QXJyYXkodG90YWxTaXplKTtcbiAgICAgICAgdmFyIGN1cnNvciA9IDA7XG4gICAgICAgIGZvciAodmFyIGkgPSAwOyBpIDwgb0Jsb2NrTGlzdC5sZW5ndGg7ICsraSkge1xuICAgICAgICAgICAgdmFyIGIgPSBvQmxvY2tMaXN0W2ldO1xuICAgICAgICAgICAgYXJyYXlDb3B5KGIsIDAsIG91dCwgY3Vyc29yLCBiLmxlbmd0aCk7XG4gICAgICAgICAgICBjdXJzb3IgKz0gYi5sZW5ndGg7XG4gICAgICAgIH1cbiAgICAgICAgcmV0dXJuIG91dC5idWZmZXI7XG4gICAgfVxufVxuXG5pZiAodHlwZW9mKG1vZHVsZSkgIT09ICd1bmRlZmluZWQnKSB7XG4gIG1vZHVsZS5leHBvcnRzID0ge1xuICAgIGluZmxhdGVCdWZmZXI6IGpzemxpYl9pbmZsYXRlX2J1ZmZlcixcbiAgICBhcnJheUNvcHk6IGFycmF5Q29weVxuICB9O1xufVxuIl19
