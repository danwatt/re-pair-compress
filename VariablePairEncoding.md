## Variable Compression of Pairs

Right now, with the most optimal compression, the output takes ~100 kb to write the listing of pairs.
This is done using 4 bytes per pair - 2 for the first value, 2 for the second

These are the last 128 pairs.

```
<[yoke],[of, oxen]>	<[yoke],[of, the, king, of, Babylon]>
<[you, ,, and],[I, will]>	<[you, ,, and],[ye, shall]>	<[you, ,, and],[ye]>
<[you, ,],[He, that]>	<[you, ,],[in]>	<[you, ,],[let, him, be]>   <[you, ,],[nor]>	<[you, ,],[or]>	<[you, ,],[saith, the, LORD]>	<[you, ,],[to]>	<[you, ,],[who]>	<[you, ,],[ye, shall]>	<[you, ,],[ye]>
<[you, ;],[but]>
<[you, by, what, authority, I, do, these],[things]>
<[you, out, of, the],[hand]>
<[you, the],[truth]>
<[you],[,, O, ye]>	<[you],[,, and, the]>	<[you],[,, and]>	<[you],[,, as]>	<[you],[,, that, ye]>
<[you],[,, that]>	<[you],[,]>	<[you],[., -VERSE-, And]>	<[you],[., -VERSE-, But]>	<[you],[., -VERSE-]>	<[you],[.]>	<[you],[:]>	<[you],[;]>
<[you],[?]>	<[you],[a]>	<[you],[all]>	<[you],[and]>	<[you],[before]>	<[you],[by, what, authority, I, do, these]>	<[you],[forth]>	<[you],[from]>
<[you],[go]>	<[you],[in]>	<[you],[into, the]>	<[you],[into]>	<[you],[not]>	<[you],[on]>	<[you],[out, of, the]>	<[you],[out]>
<[you],[that]>	<[you],[the]>	<[you],[this, day]>	<[you],[this]>	<[you],[to, be]>	<[you],[to, dwell]>	<[you],[to]>	<[you],[up]>
<[you],[with, scorpions]>	<[you],[with, the]>	<[you],[with]>
<[young, bullock],[,, one]>	<[young, bullock],[without, blemish]>
<[young, men],[,, and]>	<[young, men],[,]>
<[young],[and]> <[young],[bullock]> <[young],[children]>    <[young],[lion]>    <[young],[lions]>   <[young],[man, ,]>  <[young],[man]>	<[young],[men, shall]>	<[young],[men]>
<[young],[ones]>
<[younger],[son]>
<[your, God],[,, and, ye]>	<[your, God],[,, and]>	<[your, God],[,, which, brought, you]>	<[your, God],[;]>
<[your, burnt],[offerings]> <[your, hands],[be]>    <[your, youngest],[brother]>    <[your],[Father]>   <[your],[God]>
<[your],[brethren]> <[your],[brother]>  <[your],[burnt]>    <[your],[children]>	<[your],[daughters]>
<[your],[dwellings]>	<[your],[ear]>	<[your],[enemies]>	<[your],[evil]>	<[your],[eyes]>	<[your],[faith]>	<[your],[father]>	<[your],[fathers]>
<[your],[feet]>	<[your],[generations]>	<[your],[habitations]>	<[your],[hand]>	<[your],[hands]>	<[your],[heads]>	<[your],[heart]>	<[your],[hearts]>
<[your],[land]>	<[your],[members]>	<[your],[murmurings]>	<[your],[own]>	<[your],[peace]>	<[your],[possession]>	<[your],[seed]>	<[your],[servants]>
<[your],[sins]>	<[your],[sons, and, your, daughters]>	<[your],[sons]>	<[your],[soul]>	<[your],[souls]>	<[your],[transgressions]>	<[your],[vows]>	<[your],[way]>
<[your],[ways, and, your, doings]>	<[your],[ways]>	<[your],[wives]>	<[your],[words]>    <[your],[youngest]>
<[yourselves],[,]>	<[yourselves],[in]>	<[youth],[up]>
```

Even all the way here at the end, alphabetically, we can see that some pairs share the same first word.
For example
* Pairs starting with `your God` - there are 4 of these
* Pairs starting with just `your` (not `your God | ...`, but just `your`) - there are 44 of these.
* `<[younger],[son]>` - there is ony one pair starting with `younger`

We could use a form of RLE to eliminate the redundancies in this first pair. For `your`, with 44 of them, this could be represented as:
```
[your]:44
[brethren]
[brother]
[burnt]
...
```

Instead of 44*4 bytes (176 bytes), this could be as small as 44*2+2+1 (91 bytes), almost a 50% reduction in space.
This would result in some waste for pairs that only have a prefix that appears once

On top of this, we could apply the variable byte encoding for stopwords - this will give a huge reduction for pairs
starting with `[and the]`, and the like, as well as pairs like `<[yourselves],[in]>` - 3 bytes instead of 4.

This still doesn't get us better than just applying a general purpose compression on the results: 
GZ (798k) or BZ2 (776k), or even Snappy (828k)

The downside is the increased complexity in decoding and much more memory usage - not a problem for modern hardware,
but certainly not really practical for the Gameboy.