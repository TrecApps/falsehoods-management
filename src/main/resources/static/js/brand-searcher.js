function initBrandSearcher(brandSearcherId) {

    const optionHolder = document.getElementById(`${ brandSearcherId }-brand-option-holder`);
    var name = "";

    var brandSearchUrl = window.appConfig.brandSearchUrl;

    var mostRecentSearch = new Date(); // Used to track the most recent search.

    var elementItemSetting = window.appConfig.elementItemSetting;

    function onInputChange(event){
        name = event.target.value;

        doSearch();
    }

    function recalibrateSearchResults(startDate, results){
        // If this search is older than the most recent search, return as it is stale
        if(mostRecentSearch.getTime() > startDate.getTime()) return;

        mostRecentSearch = startDate;

        optionHolder.replaceChildren();

        // Brand brand;
        //     names
        // String image;

        for(let result of results){
            let optionDiv = document.createElement("div");
            optionDiv.classList.add("brand-option");
            optionDiv.classList.add("element-item");
            optionDiv.classList.add(elementItemSetting);
            optionDiv.addEventListener("click", () => {
                recalibrateSearchResults(new Date(), [])
                window[`${brandSearcherId}_onBrandSelected`](result);
            });
            optionHolder.appendChild(optionDiv);

            let imageHolder = document.createElement("div");
            imageHolder.classList.add("brand-img-holder");
            optionDiv.appendChild(imageHolder);

            let image = document.createElement("img");
            image.src = result.image;
            image.alt = `image of ${result.brand.names[0]}`;
            imageHolder.appendChild(image);

            let brandHolder = document.createElement("div");
            brandHolder.classList.add("brand-name-holder");
            optionDiv.appendChild(brandHolder);
            let nameText = document.createElement("h5");
            nameText.classList.add("brand-name");
            nameText.textContent = result.brand.names[0];
            brandHolder.appendChild(nameText);
        }
    }

    function doSearch(){
        if(name.length < 4) return;

        let start = new Date();

        const url = new URL(`${brandSearchUrl}/brands-api`);
        const params = { query: name };

        // Automatically handles encoding and '?' or '&' symbols
        url.search = new URLSearchParams(params).toString();


        fetch(url, {
            method: "GET",
        }).then(async (response) => {
            if(response.status != 200) throw ""

            let results = await response.json();

            recalibrateSearchResults(start, results);
        })

    }

    document.getElementById(`${brandSearcherId}-brand-input`).addEventListener('input',onInputChange)



}